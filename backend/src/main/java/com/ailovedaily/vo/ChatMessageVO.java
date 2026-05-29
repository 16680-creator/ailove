package com.ailovedaily.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {

    private Long id;

    private Long coupleId;

    private Long userId;

    private String role;

    private String content;

    private String imageUrl;

    private Integer messageType;

    private String conversationId;

    private LocalDateTime createTime;
}
