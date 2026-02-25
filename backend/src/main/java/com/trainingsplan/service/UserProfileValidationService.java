package com.trainingsplan.service;

import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserProfileValidationService {

    public ProfileCompletionDto getProfileCompletion(User user) {
        List<String> missingFields = new ArrayList<>();
        if (user == null) {
            return new ProfileCompletionDto(false, List.of("user"), "Benutzer ist nicht authentifiziert.");
        }

        if (isBlank(user.getFirstName())) missingFields.add("firstName");
        if (isBlank(user.getLastName())) missingFields.add("lastName");
        if (user.getDateOfBirth() == null) missingFields.add("dateOfBirth");
        if (user.getHeightCm() == null || user.getHeightCm() <= 0) missingFields.add("heightCm");
        if (user.getWeightKg() == null || user.getWeightKg() <= 0) missingFields.add("weightKg");
        if (user.getMaxHeartRate() == null || user.getMaxHeartRate() <= 0) missingFields.add("maxHeartRate");
        if (user.getHrRest() == null || user.getHrRest() <= 0) missingFields.add("hrRest");
        if (isBlank(user.getGender())) missingFields.add("gender");

        boolean complete = missingFields.isEmpty();
        String message = complete
                ? "Profil ist vollstaendig fuer Metrik-Berechnungen."
                : "Bitte Profil vervollstaendigen. Fehlende Felder: " + String.join(", ", missingFields);

        return new ProfileCompletionDto(complete, missingFields, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
