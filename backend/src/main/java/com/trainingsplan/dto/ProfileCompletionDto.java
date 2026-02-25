package com.trainingsplan.dto;

import java.util.List;

public record ProfileCompletionDto(boolean complete, List<String> missingFields, String message) {}
