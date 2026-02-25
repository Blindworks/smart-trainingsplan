package com.trainingsplan.service;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createUser(String username, String email) {
        User user = new User(username, email, LocalDateTime.now());
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User updateUser(Long id, String username, String email,
                           String firstName, String lastName,
                           LocalDate dateOfBirth, Integer heightCm, Double weightKg,
                           Integer maxHeartRate, Integer hrRest, String gender, String status) {
        User user = findById(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDateOfBirth(dateOfBirth);
        user.setHeightCm(heightCm);
        user.setWeightKg(weightKg);
        user.setMaxHeartRate(maxHeartRate);
        user.setHrRest(hrRest);
        user.setGender(gender);
        if (status != null && !status.isBlank()) {
            user.setStatus(UserStatus.valueOf(status));
        }
        return userRepository.save(user);
    }
}
