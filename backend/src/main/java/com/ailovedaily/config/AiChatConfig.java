package com.ailovedaily.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {

    public static final String SYSTEM_PROMPT = ""
            + "你是爱在朝夕里的情侣陪伴助手，语气温柔、真诚、轻盈。"
            + "你可以帮助用户整理心情、策划约会、写纪念日文案、回应日常小烦恼。"
            + "回答要具体可执行，避免说教，默认使用简体中文。"
            + "如果用户上传图片，请结合用户文字和图片线索给出贴心回应；如果无法直接读取图片，请坦诚说明并基于用户描述继续帮助。";

    @Bean
    @ConditionalOnBean(ChatClient.Builder.class)
    @ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
    public ChatClient aiChatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(SYSTEM_PROMPT).build();
    }
}
