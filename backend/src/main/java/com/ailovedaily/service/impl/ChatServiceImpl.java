package com.ailovedaily.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ailovedaily.dto.ChatMessageDTO;
import com.ailovedaily.entity.ChatMessage;
import com.ailovedaily.exception.BusinessException;
import com.ailovedaily.mapper.ChatMessageMapper;
import com.ailovedaily.service.ChatService;
import com.ailovedaily.vo.ChatConversationVO;
import com.ailovedaily.vo.ChatMessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int CONTEXT_SIZE = 20;
    private static final int MAX_HISTORY_SIZE = 100;
    private static final int MAX_CONVERSATION_SCAN = 300;

    private final ChatMessageMapper chatMessageMapper;
    private final ObjectProvider<ChatClient> chatClientProvider;

    @Value("${ai.enabled:false}")
    private Boolean aiEnabled;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long userId, Long coupleId, ChatMessageDTO chatMessageDTO) {
        assertCanChat(coupleId, chatMessageDTO);

        String conversationId = normalizeConversationId(chatMessageDTO.getConversationId());
        ChatMessage userMessage = saveUserMessage(userId, coupleId, conversationId, chatMessageDTO);

        String answer;
        try {
            answer = CompletableFuture.supplyAsync(
                    () -> callAi(buildPrompt(coupleId, conversationId), chatMessageDTO)
            ).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("AI chat call timed out, using fallback");
            answer = fallbackAnswer(chatMessageDTO);
        } catch (Exception e) {
            log.error("AI chat call failed", e);
            answer = fallbackAnswer(chatMessageDTO);
        }

        ChatMessage assistantMessage = saveAssistantMessage(coupleId, conversationId, answer);

        log.info("AI chat completed: userMessageId={}, assistantMessageId={}", userMessage.getId(), assistantMessage.getId());
        return toVO(assistantMessage);
    }

    @Override
    public SseEmitter streamMessage(Long userId, Long coupleId, ChatMessageDTO chatMessageDTO) {
        assertCanChat(coupleId, chatMessageDTO);

        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.warn("SSE error: {}", e.getMessage()));
        String conversationId = normalizeConversationId(chatMessageDTO.getConversationId());
        saveUserMessage(userId, coupleId, conversationId, chatMessageDTO);

        Flux<String> stream = callAiStream(buildPrompt(coupleId, conversationId), chatMessageDTO);
        AtomicReference<StringBuilder> fullAnswer = new AtomicReference<>(new StringBuilder());

        stream.subscribe(chunk -> {
            if (StrUtil.isBlank(chunk)) {
                return;
            }
            fullAnswer.get().append(chunk);
            sendSse(emitter, "message", chunk);
        }, error -> {
            log.error("AI chat stream failed", error);
            String fallback = fallbackAnswer(chatMessageDTO);
            fullAnswer.get().append(fallback);
            sendSse(emitter, "message", fallback);
            ChatMessage saved = saveAssistantMessage(coupleId, conversationId, fallback);
            sendSse(emitter, "done", toVO(saved));
            emitter.complete();
        }, () -> {
            String content = cleanAnswer(fullAnswer.get().toString());
            if (StrUtil.isBlank(content)) {
                content = fallbackAnswer(chatMessageDTO);
                sendSse(emitter, "message", content);
            }
            ChatMessage saved = saveAssistantMessage(coupleId, conversationId, content);
            sendSse(emitter, "done", toVO(saved));
            emitter.complete();
        });

        return emitter;
    }

    @Override
    public List<ChatMessageVO> getHistory(Long coupleId, String conversationId, Integer limit) {
        if (coupleId == null) {
            throw new BusinessException(400, "请先绑定情侣关系");
        }
        if (StrUtil.isBlank(conversationId)) {
            throw new BusinessException(400, "会话ID不能为空");
        }

        int pageSize = Math.min(limit == null ? 50 : Math.max(limit, 1), MAX_HISTORY_SIZE);
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getCoupleId, coupleId)
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + pageSize));
        Collections.reverse(messages);
        return messages.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ChatConversationVO> getConversations(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException(400, "请先绑定情侣关系");
        }

        List<ChatMessage> recentMessages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getCoupleId, coupleId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + MAX_CONVERSATION_SCAN));

        Map<String, ChatConversationVO> conversations = new LinkedHashMap<>();
        for (ChatMessage message : recentMessages) {
            String conversationId = message.getConversationId();
            if (StrUtil.isBlank(conversationId)) {
                continue;
            }

            ChatConversationVO conversation = conversations.get(conversationId);
            if (conversation == null) {
                conversation = new ChatConversationVO();
                conversation.setConversationId(conversationId);
                conversation.setTitle(buildTitle(message.getContent()));
                conversation.setLastMessage(message.getContent());
                conversation.setLastRole(message.getRole());
                conversation.setLastTime(message.getCreateTime());
                conversation.setMessageCount(0);
                conversations.put(conversationId, conversation);
            }
            conversation.setMessageCount(conversation.getMessageCount() + 1);
        }
        return new ArrayList<>(conversations.values());
    }

    private void assertCanChat(Long coupleId, ChatMessageDTO chatMessageDTO) {
        if (coupleId == null) {
            throw new BusinessException(400, "请先绑定情侣关系");
        }
        if (chatMessageDTO == null ||
                (StrUtil.isBlank(chatMessageDTO.getContent()) && StrUtil.isBlank(chatMessageDTO.getImageUrl()))) {
            throw new BusinessException(400, "请输入消息内容或选择一张图片");
        }
    }

    private String normalizeConversationId(String conversationId) {
        return StrUtil.isBlank(conversationId) ? IdUtil.fastSimpleUUID() : conversationId.trim();
    }

    private ChatMessage saveUserMessage(Long userId, Long coupleId, String conversationId, ChatMessageDTO dto) {
        ChatMessage message = new ChatMessage();
        message.setCoupleId(coupleId);
        message.setUserId(userId);
        message.setRole("user");
        message.setContent(StrUtil.blankToDefault(dto.getContent(), ""));
        message.setImageUrl(dto.getImageUrl());
        message.setMessageType(StrUtil.isBlank(dto.getImageUrl()) ? 0 : 1);
        message.setConversationId(conversationId);
        chatMessageMapper.insert(message);
        return message;
    }

    private ChatMessage saveAssistantMessage(Long coupleId, String conversationId, String content) {
        ChatMessage message = new ChatMessage();
        message.setCoupleId(coupleId);
        message.setUserId(0L);
        message.setRole("assistant");
        message.setContent(cleanAnswer(content));
        message.setMessageType(0);
        message.setConversationId(conversationId);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    private String buildPrompt(Long coupleId, String conversationId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getCoupleId, coupleId)
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + CONTEXT_SIZE));
        Collections.reverse(messages);

        StringBuilder prompt = new StringBuilder();
        prompt.append("以下是最近对话，请自然接续回复。\n\n");
        for (ChatMessage message : messages) {
            prompt.append("user".equals(message.getRole()) ? "用户" : "助手")
                    .append(": ")
                    .append(StrUtil.blankToDefault(message.getContent(), ""));
            if (StrUtil.isNotBlank(message.getImageUrl())) {
                prompt.append("\n[用户附图] ").append(message.getImageUrl());
            }
            prompt.append("\n");
        }
        return prompt.toString();
    }

    private String callAi(String prompt, ChatMessageDTO dto) {
        if (!canUseAi()) {
            return fallbackAnswer(dto);
        }

        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            return fallbackAnswer(dto);
        }

        try {
            String content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            content = cleanAnswer(content);
            return StrUtil.isBlank(content) ? fallbackAnswer(dto) : content;
        } catch (Exception exception) {
            log.error("AI chat call failed", exception);
            return fallbackAnswer(dto);
        }
    }

    private Flux<String> callAiStream(String prompt, ChatMessageDTO dto) {
        if (!canUseAi()) {
            return Flux.just(fallbackAnswer(dto));
        }

        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            return Flux.just(fallbackAnswer(dto));
        }

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .map(this::cleanChunk)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> {
                        log.warn("AI chat stream error/timeout: {}", e.getMessage());
                        return Flux.just(fallbackAnswer(dto));
                    });
        } catch (Exception exception) {
            log.error("AI chat stream setup failed", exception);
            return Flux.just(fallbackAnswer(dto));
        }
    }

    private boolean canUseAi() {
        return Boolean.TRUE.equals(aiEnabled) && StrUtil.isNotBlank(aiApiKey);
    }

    private String fallbackAnswer(ChatMessageDTO dto) {
        if (StrUtil.isNotBlank(dto.getImageUrl()) && StrUtil.isBlank(dto.getContent())) {
            return "我看到你发来了一张图片。现在 AI 还没有连上，我先替你把这份瞬间收好，等配置好模型后就能一起聊它啦。";
        }
        return "我已经收到啦。当前 AI 服务还没有配置好，等接入模型后，我会在这里陪你们继续把日常聊得更有温度。";
    }

    private String cleanChunk(String chunk) {
        if (chunk == null) {
            return "";
        }
        return chunk.replaceAll("</?think[^>]*>", "");
    }

    private String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        String content = answer.replaceAll("<think[^>]*>[\\s\\S]*?</think\\s*>", "");
        content = content.replaceAll("</?think[^>]*>", "");
        return content.trim();
    }

    private String buildTitle(String content) {
        if (StrUtil.isBlank(content)) {
            return "新的对话";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 18 ? normalized.substring(0, 18) + "..." : normalized;
    }

    private ChatMessageVO toVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtil.copyProperties(message, vo);
        return vo;
    }

    private void sendSse(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException exception) {
            log.warn("SSE send failed: {}", exception.getMessage());
        }
    }
}
