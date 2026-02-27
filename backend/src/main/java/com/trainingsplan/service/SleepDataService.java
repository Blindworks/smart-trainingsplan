package com.trainingsplan.service;

import com.trainingsplan.entity.SleepData;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.SleepDataRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SleepDataService {

    @Autowired
    private SleepDataRepository sleepDataRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public List<SleepData> getAllForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();
        return sleepDataRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    public Optional<SleepData> getLatestForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return sleepDataRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
    }

    @Transactional
    public SleepData create(SleepData sleepData) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        sleepData.setUser(user);
        return sleepDataRepository.save(sleepData);
    }

    @Transactional
    public SleepData update(Long id, SleepData updated) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        SleepData existing = sleepDataRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sleep data not found: " + id));

        existing.setRecordedAt(updated.getRecordedAt());
        existing.setSleepScore(updated.getSleepScore());
        existing.setSleepScore7Days(updated.getSleepScore7Days());
        existing.setRestingHeartRate(updated.getRestingHeartRate());
        existing.setBodyBattery(updated.getBodyBattery());
        existing.setSpO2(updated.getSpO2());
        existing.setBreathingRate(updated.getBreathingRate());
        existing.setHrvStatus(updated.getHrvStatus());
        existing.setSleepQuality(updated.getSleepQuality());
        existing.setSleepDurationMinutes(updated.getSleepDurationMinutes());
        existing.setSleepNeedMinutes(updated.getSleepNeedMinutes());
        existing.setBedtime(updated.getBedtime());
        existing.setWakeTime(updated.getWakeTime());

        return sleepDataRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        SleepData sleepData = sleepDataRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sleep data not found: " + id));
        sleepDataRepository.delete(sleepData);
    }

    @Transactional
    public Map<String, Integer> importFromGarminCsv(MultipartFile file) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // skip header (may have BOM)
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 12) { skipped++; continue; }
                try {
                    LocalDate date = LocalDate.parse(cols[0].trim());

                    SleepData entry = sleepDataRepository
                            .findByUserIdAndRecordedAt(user.getId(), date)
                            .orElseGet(() -> {
                                SleepData s = new SleepData();
                                s.setUser(user);
                                return s;
                            });

                    entry.setRecordedAt(date);
                    entry.setSleepScore(parseIntOrNull(cols[1]));
                    entry.setRestingHeartRate(parseIntOrNull(cols[2]));
                    entry.setBodyBattery(parseIntOrNull(cols[3]));
                    entry.setSpO2(parseDoubleOrNull(cols[4]));
                    entry.setBreathingRate(parseDoubleOrNull(cols[5]));
                    entry.setHrvStatus(cols[6].trim().equals("--") || cols[6].isBlank() ? null : cols[6].trim());
                    entry.setSleepQuality(cols[7].isBlank() ? null : cols[7].trim());
                    entry.setSleepDurationMinutes(parseDurationMinutes(cols[8]));
                    entry.setSleepNeedMinutes(parseDurationMinutes(cols[9]));
                    entry.setBedtime(parseTime(cols[10]));
                    entry.setWakeTime(parseTime(cols[11]));

                    sleepDataRepository.save(entry);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV-Datei konnte nicht gelesen werden");
        }

        return Map.of("imported", imported, "skipped", skipped);
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equals("--")) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equals("--")) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseDurationMinutes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equals("--")) return null;
        int hours = 0, mins = 0;
        Matcher m = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)min)?").matcher(s);
        if (m.find()) {
            if (m.group(1) != null) hours = Integer.parseInt(m.group(1));
            if (m.group(2) != null) mins = Integer.parseInt(m.group(2));
        }
        int total = hours * 60 + mins;
        return total == 0 ? null : total;
    }

    private LocalTime parseTime(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equals("--")) return null;
        try {
            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            return LocalTime.of(h, min);
        } catch (Exception e) { return null; }
    }
}
