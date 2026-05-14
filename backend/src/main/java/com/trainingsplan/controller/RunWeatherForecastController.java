package com.trainingsplan.controller;

import com.trainingsplan.dto.RunWeatherForecastDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.RunWeatherForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Short-term run-weather forecast for the authenticated user. The dashboard surfaces this as
 * a "should I head out for a run in the next few hours?" widget.
 *
 * <p>The forecast is location-bound: the user's stored coordinates (set automatically from
 * the address in settings, or manually via the map picker) drive the query against the DWD
 * ICON model.</p>
 */
@RestController
@RequestMapping("/api/weather")
public class RunWeatherForecastController {

    private final RunWeatherForecastService forecastService;
    private final SecurityUtils securityUtils;

    public RunWeatherForecastController(RunWeatherForecastService forecastService, SecurityUtils securityUtils) {
        this.forecastService = forecastService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/forecast")
    public ResponseEntity<RunWeatherForecastDto> getForecast() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        RunWeatherForecastDto dto = forecastService.getForecastForUser(user);
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }
}
