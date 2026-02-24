package com.trainingsplan.service;

import com.trainingsplan.entity.BloodPressure;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.BloodPressureRepository;
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
public class BloodPressureService {

    @Autowired
    private BloodPressureRepository bloodPressureRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<BloodPressure> getAllForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        return bloodPressureRepository.findByUserIdOrderByMeasuredAtDesc(userId);
    }

    public Optional<BloodPressure> getLatestForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return bloodPressureRepository.findTopByUserIdOrderByMeasuredAtDesc(userId);
    }

    @Transactional
    public BloodPressure create(BloodPressure bp) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        bp.setUser(user);
        return bloodPressureRepository.save(bp);
    }

    @Transactional
    public BloodPressure update(Long id, BloodPressure updated) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        BloodPressure existing = bloodPressureRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Blood pressure measurement not found: " + id));

        existing.setMeasuredAt(updated.getMeasuredAt());
        existing.setSystolicPressure(updated.getSystolicPressure());
        existing.setDiastolicPressure(updated.getDiastolicPressure());
        existing.setPulseAtMeasurement(updated.getPulseAtMeasurement());
        existing.setNotes(updated.getNotes());

        return bloodPressureRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        BloodPressure bp = bloodPressureRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Blood pressure measurement not found: " + id));
        bloodPressureRepository.delete(bp);
    }
}
