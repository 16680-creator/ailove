package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationVO {

    private String conversationId;

    private String title;

    private String lastMessage;

    private String lastRole;

    private Integer messageCount;

    private LocalDateTime lastTime;
}
