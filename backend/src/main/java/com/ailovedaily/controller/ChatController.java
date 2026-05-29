package com.ailovedaily.controller;

import com.ailovedaily.dto.ChatMessageDTO;
import com.ailovedaily.service.ChatService;
import com.ailovedaily.vo.ChatConversationVO;
import com.ailovedaily.vo.ChatMessageVO;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "情侣 AI 助手聊天接口")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @Operation(summary = "发送聊天消息", description = "非流式发送消息并返回 AI 回复")
    public ResultVO<ChatMessageVO> sendMessage(@RequestAttribute("userId") Long userId,
                                               @RequestAttribute("coupleId") Long coupleId,
                                               @Valid @RequestBody ChatMessageDTO chatMessageDTO) {
        ChatMessageVO message = chatService.sendMessage(userId, coupleId, chatMessageDTO);
        return ResultVO.success(message);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送聊天消息", description = "通过 SSE 流式返回 AI 回复")
    public SseEmitter streamMessage(@RequestAttribute("userId") Long userId,
                                    @RequestAttribute("coupleId") Long coupleId,
                                    @Valid @RequestBody ChatMessageDTO chatMessageDTO) {
        return chatService.streamMessage(userId, coupleId, chatMessageDTO);
    }

    @GetMapping("/history")
    @Operation(summary = "获取聊天历史", description = "按会话获取聊天记录")
    public ResultVO<List<ChatMessageVO>> getHistory(@RequestAttribute("coupleId") Long coupleId,
                                                    @RequestParam String conversationId,
                                                    @RequestParam(defaultValue = "50") Integer limit) {
        List<ChatMessageVO> history = chatService.getHistory(coupleId, conversationId, limit);
        return ResultVO.success(history);
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表", description = "获取最近 AI 对话列表")
    public ResultVO<List<ChatConversationVO>> getConversations(@RequestAttribute("coupleId") Long coupleId) {
        List<ChatConversationVO> conversations = chatService.getConversations(coupleId);
        return ResultVO.success(conversations);
    }
}
