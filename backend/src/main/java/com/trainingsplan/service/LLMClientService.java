package com.trainingsplan.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class LLMClientService {

    private final ChatLanguageModel chatLanguageModel;

    public LLMClientService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public String generateText(String prompt) {
        return chatLanguageModel.generate(prompt);
    }
}

