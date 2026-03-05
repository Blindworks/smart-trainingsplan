package com.trainingsplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIPlanResponseParserTest {

    private AIPlanResponseParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new AIPlanResponseParser(objectMapper, validator, new AIPlanValidator());
    }

    @Test
    void parse_validResponse_returnsPlanDto() {
        String json = """
                {
                  "weekStartDate": "2026-03-02",
                  "status": "DRAFT",
                  "days": [
                    {
                      "date": "2026-03-02",
                      "workouts": [
                        {
                          "type": "EASY",
                          "targetZone": "Z2",
                          "durationMinutes": 45,
                          "description": "Easy run"
                        }
                      ]
                    }
                  ]
                }
                """;

        var result = parser.parse(json);

        assertNotNull(result);
        assertEquals(1, result.getDays().size());
        assertEquals("EASY", result.getDays().get(0).getWorkouts().get(0).getType().name());
    }

    @Test
    void parse_invalidJson_throwsAIResponseParsingException() {
        String invalidJson = "{ \"weekStartDate\": \"2026-03-02\", \"days\": [";

        AIResponseParsingException ex = assertThrows(
                AIResponseParsingException.class,
                () -> parser.parse(invalidJson)
        );

        assertTrue(ex.getMessage().contains("Invalid AI response JSON"));
    }

    @Test
    void parse_unexpectedField_throwsAIResponseParsingException() {
        String jsonWithUnexpectedField = """
                {
                  "weekStartDate": "2026-03-02",
                  "days": [
                    {
                      "date": "2026-03-02",
                      "workouts": [
                        {
                          "type": "EASY",
                          "targetZone": "Z2",
                          "durationMinutes": 30
                        }
                      ]
                    }
                  ],
                  "unexpected": "value"
                }
                """;

        AIResponseParsingException ex = assertThrows(
                AIResponseParsingException.class,
                () -> parser.parse(jsonWithUnexpectedField)
        );

        assertTrue(ex.getMessage().contains("Unexpected field in AI response"));
        assertTrue(ex.getMessage().contains("unexpected"));
    }

    @Test
    void parse_missingRequiredField_throwsAIResponseParsingException() {
        String missingRequiredFieldJson = """
                {
                  "days": [
                    {
                      "date": "2026-03-02",
                      "workouts": [
                        {
                          "type": "EASY",
                          "targetZone": "Z2",
                          "durationMinutes": 30
                        }
                      ]
                    }
                  ]
                }
                """;

        AIResponseParsingException ex = assertThrows(
                AIResponseParsingException.class,
                () -> parser.parse(missingRequiredFieldJson)
        );

        assertTrue(ex.getMessage().contains("Missing or invalid required fields"));
        assertTrue(ex.getMessage().contains("weekStartDate"));
    }

    @Test
    void parse_invalidTargetZone_throwsAIResponseParsingException() {
        String invalidZoneJson = """
                {
                  "weekStartDate": "2026-03-02",
                  "days": [
                    {
                      "date": "2026-03-02",
                      "workouts": [
                        {
                          "type": "EASY",
                          "targetZone": "Z9",
                          "durationMinutes": 30
                        }
                      ]
                    }
                  ]
                }
                """;

        AIResponseParsingException ex = assertThrows(
                AIResponseParsingException.class,
                () -> parser.parse(invalidZoneJson)
        );

        assertTrue(ex.getMessage().contains("AI plan validation failed"));
        assertTrue(ex.getMessage().contains("targetZone"));
    }
}