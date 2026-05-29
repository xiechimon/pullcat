package com.pullcat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类，用于注册 ChatClient 等 AI 相关 Bean。
 */
@Configuration
public class SpringAiConfig {

    /**
     * 创建 ChatClient Bean，基于 OpenAI Chat Model 构建。
     *
     * @param openAiChatModel OpenAI 聊天模型实例
     * @return 配置好的 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
