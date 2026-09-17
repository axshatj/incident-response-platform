package com.irp.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AgentConfig {

    @Bean
    ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    @Bean
    RestClient incidentRestClient(RestClient.Builder builder,
                                  @org.springframework.beans.factory.annotation.Value("${irp.incident-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
