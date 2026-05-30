package com.pullcat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class SpringAiConfig {

    @Value("${pullcat.llm.light-model:deepseek-chat}")
    private String lightModel;

    @Value("${pullcat.llm.heavy-model:deepseek-reasoner}")
    private String heavyModel;

    @Bean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    @Primary
    @Qualifier("lightChatModel")
    public OpenAiChatModel lightChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi,
                OpenAiChatOptions.builder().model(lightModel).build());
    }

    @Bean
    @Qualifier("heavyChatModel")
    public OpenAiChatModel heavyChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi,
                OpenAiChatOptions.builder().model(heavyModel).build());
    }

    @Bean
    @Qualifier("lightChatClient")
    public ChatClient lightChatClient(@Qualifier("lightChatModel") OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("heavyChatClient")
    public ChatClient heavyChatClient(@Qualifier("heavyChatModel") OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("analysisExecutor")
    public ExecutorService analysisExecutor() {
        return Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "analysis-worker");
            t.setDaemon(true);
            return t;
        });
    }
}
