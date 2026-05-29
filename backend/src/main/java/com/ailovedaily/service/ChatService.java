package com.ailovedaily.service;

import com.ailovedaily.dto.ChatMessageDTO;
import com.ailovedaily.vo.ChatConversationVO;
import com.ailovedaily.vo.ChatMessageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    ChatMessageVO sendMessage(Long userId, Long coupleId, ChatMessageDTO chatMessageDTO);

    SseEmitter streamMessage(Long userId, Long coupleId, ChatMessageDTO chatMessageDTO);

    List<ChatMessageVO> getHistory(Long coupleId, String conversationId, Integer limit);

    List<ChatConversationVO> getConversations(Long coupleId);
}
