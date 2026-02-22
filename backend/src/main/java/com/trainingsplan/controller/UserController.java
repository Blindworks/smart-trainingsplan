package com.trainingsplan.controller;

import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public UserController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    public record CreateUserRequest(String username, String email) {}

    public record UpdateUserRequest(
            String username,
            String email,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Integer heightCm,
            Double weightKg,
            Integer maxHeartRate
    ) {}

    @GetMapping("/me")
    public ResponseEntity<User> getMe() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User created = userService.createUser(request.username(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            User updated = userService.updateUser(id, request.username(), request.email(),
                    request.firstName(), request.lastName(),
                    request.dateOfBirth(), request.heightCm(), request.weightKg(), request.maxHeartRate());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
