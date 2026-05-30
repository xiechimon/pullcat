package com.pullcat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class PullcatApplication {

    @Value("${pullcat.llm.light-model:deepseek-chat}")
    private String lightModel;

    @Value("${pullcat.llm.heavy-model:deepseek-reasoner}")
    private String heavyModel;

    public static void main(String[] args) {
        SpringApplication.run(PullcatApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logModelConfig() {
        System.out.println("[pullcat] Light model: " + lightModel);
        System.out.println("[pullcat] Heavy model: " + heavyModel);
    }
}
