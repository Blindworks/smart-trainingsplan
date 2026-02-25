package com.trainingsplan.controller;

import com.trainingsplan.dto.DashboardDto;
import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.DashboardService;
import com.trainingsplan.service.UserProfileValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;
    private final UserProfileValidationService userProfileValidationService;

    public DashboardController(DashboardService dashboardService, SecurityUtils securityUtils,
                               UserProfileValidationService userProfileValidationService) {
        this.dashboardService = dashboardService;
        this.securityUtils = securityUtils;
        this.userProfileValidationService = userProfileValidationService;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
        if (!completion.complete()) {
            return ResponseEntity.badRequest().body(completion);
        }

        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }
}
