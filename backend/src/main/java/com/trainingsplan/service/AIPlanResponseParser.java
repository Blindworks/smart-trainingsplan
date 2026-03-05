package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.trainingsplan.dto.AITrainingPlanDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AIPlanResponseParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AIPlanResponseParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AITrainingPlanDTO parse(String llmJsonResponse) {
        if (llmJsonResponse == null || llmJsonResponse.isBlank()) {
            throw new AIResponseParsingException("AI response JSON is missing");
        }

        try {
            AITrainingPlanDTO plan = objectMapper
                    .readerFor(AITrainingPlanDTO.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(llmJsonResponse);

            validate(plan);
            return plan;
        } catch (UnrecognizedPropertyException e) {
            throw new AIResponseParsingException("Unexpected field in AI response: " + e.getPropertyName(), e);
        } catch (JsonProcessingException e) {
            throw new AIResponseParsingException("Invalid AI response JSON", e);
        }
    }

    private void validate(AITrainingPlanDTO plan) {
        Set<ConstraintViolation<AITrainingPlanDTO>> violations = validator.validate(plan);
        if (violations.isEmpty()) {
            return;
        }

        String validationMessage = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        throw new AIResponseParsingException("Missing or invalid required fields: " + validationMessage);
    }
}
