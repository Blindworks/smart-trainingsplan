package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.trainingsplan.dto.AITrainingPlanDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AIPlanResponseParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AIPlanValidator aiPlanValidator;

    @Autowired
    public AIPlanResponseParser(ObjectMapper objectMapper, Validator validator, AIPlanValidator aiPlanValidator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.aiPlanValidator = aiPlanValidator;
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

            validateBeanConstraints(plan);
            validatePlanRules(plan);
            return plan;
        } catch (UnrecognizedPropertyException e) {
            throw new AIResponseParsingException("Unexpected field in AI response: " + e.getPropertyName(), e);
        } catch (JsonProcessingException e) {
            throw new AIResponseParsingException("Invalid AI response JSON", e);
        }
    }

    private void validateBeanConstraints(AITrainingPlanDTO plan) {
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

    private void validatePlanRules(AITrainingPlanDTO plan) {
        ValidationResult result = aiPlanValidator.validate(plan);
        if (result.isValid()) {
            return;
        }

        throw new AIResponseParsingException("AI plan validation failed: " + String.join("; ", result.getErrors()));
    }
}
