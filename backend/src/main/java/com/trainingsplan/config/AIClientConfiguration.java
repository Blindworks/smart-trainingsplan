package com.trainingsplan.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class AIClientConfiguration {

    @Bean
    public ChatLanguageModel chatLanguageModel(
        @Value("${pacr.ai.model}") String model,
        @Value("${pacr.ai.apiKey}") String apiKey
    ) {
        return OpenAiChatModel.builder()
            .modelName(model)
            .apiKey(apiKey)
            .build();
    }
}

