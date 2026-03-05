package com.trainingsplan.controller;

import com.trainingsplan.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/training-plan")
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "false", matchIfMissing = true)
public class AITrainingPlanDisabledController {

    private static final Logger log = LoggerFactory.getLogger(AITrainingPlanDisabledController.class);
    private static final String DISABLED_MESSAGE =
            "AI training plan feature is disabled. Set pacr.ai.enabled=true to enable it.";

    @PostMapping("/generate")
    public ResponseEntity<MessageResponse> generate(Authentication authentication) {
        log.warn("event=ai_training_plan_disabled endpoint=/api/ai/training-plan/generate user={} reason=pacr.ai.enabled_false",
                authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MessageResponse(DISABLED_MESSAGE));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<MessageResponse> getPlan(@PathVariable String planId, Authentication authentication) {
        log.warn("event=ai_training_plan_disabled endpoint=/api/ai/training-plan/{} user={} reason=pacr.ai.enabled_false",
                planId, authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MessageResponse(DISABLED_MESSAGE));
    }
}
