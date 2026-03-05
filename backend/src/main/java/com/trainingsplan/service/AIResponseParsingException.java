package com.trainingsplan.service;

/**
 * Thrown when an LLM response cannot be parsed into a valid AI training plan DTO.
 */
public class AIResponseParsingException extends RuntimeException {

    public AIResponseParsingException(String message) {
        super(message);
    }

    public AIResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
