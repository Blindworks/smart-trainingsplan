package com.trainingsplan.service;

import com.trainingsplan.entity.BodyMeasurement;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.BodyMeasurementRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BodyMeasurementService {

    @Autowired
    private BodyMeasurementRepository bodyMeasurementRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<BodyMeasurement> getAllForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        return bodyMeasurementRepository.findByUserIdOrderByMeasuredAtDesc(userId);
    }

    public Optional<BodyMeasurement> getLatestForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return bodyMeasurementRepository.findTopByUserIdOrderByMeasuredAtDesc(userId);
    }

    @Transactional
    public BodyMeasurement create(BodyMeasurement measurement) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        measurement.setUser(user);
        return bodyMeasurementRepository.save(measurement);
    }

    @Transactional
    public BodyMeasurement update(Long id, BodyMeasurement updated) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        BodyMeasurement existing = bodyMeasurementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Measurement not found: " + id));

        existing.setMeasuredAt(updated.getMeasuredAt());
        existing.setWeightKg(updated.getWeightKg());
        existing.setFatPercentage(updated.getFatPercentage());
        existing.setWaterPercentage(updated.getWaterPercentage());
        existing.setMuscleMassKg(updated.getMuscleMassKg());
        existing.setBoneMassKg(updated.getBoneMassKg());
        existing.setVisceralFatLevel(updated.getVisceralFatLevel());
        existing.setMetabolicAge(updated.getMetabolicAge());
        existing.setBmi(updated.getBmi());
        existing.setNotes(updated.getNotes());

        return bodyMeasurementRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        BodyMeasurement measurement = bodyMeasurementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Measurement not found: " + id));
        bodyMeasurementRepository.delete(measurement);
    }
}
