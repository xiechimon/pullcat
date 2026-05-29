package com.pullcat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类，负责创建和注册 ChatClient Bean，作为与 LLM 交互的统一入口。
 */
@Configuration
public class SpringAiConfig {

    /**
     * 创建 Spring AI 聊天客户端 Bean，用于与大语言模型交互。
     *
     * @param openAiChatModel Spring AI 的 OpenAI 聊天模型实现
     * @return 构建好的 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
