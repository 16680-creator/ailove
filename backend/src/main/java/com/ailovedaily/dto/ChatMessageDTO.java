package com.ailovedaily.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageDTO {

    @Size(max = 4000, message = "消息内容不能超过4000个字符")
    private String content;

    @Size(max = 500, message = "图片地址不能超过500个字符")
    private String imageUrl;

    @Size(max = 64, message = "会话ID不能超过64个字符")
    private String conversationId;
}
