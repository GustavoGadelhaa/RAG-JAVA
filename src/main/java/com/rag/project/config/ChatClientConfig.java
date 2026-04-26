package com.rag.project.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Criando ChatClient com ChatModel: {}", chatModel.getClass().getSimpleName());
        return ChatClient.builder(chatModel).build();
    }
}