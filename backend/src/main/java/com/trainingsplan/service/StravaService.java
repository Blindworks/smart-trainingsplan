package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.dto.StravaStatusDto;
import com.trainingsplan.entity.StravaToken;
import com.trainingsplan.repository.StravaTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class StravaService {

    @Value("${strava.client-id}")
    private String clientId;

    @Value("${strava.client-secret}")
    private String clientSecret;

    @Value("${strava.redirect-uri}")
    private String redirectUri;

    @Value("${strava.frontend-url}")
    private String frontendUrl;

    private final StravaTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public StravaService(StravaTokenRepository tokenRepository, ObjectMapper objectMapper) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public String getAuthorizationUrl() {
        return "https://www.strava.com/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=activity:read_all";
    }

    public void exchangeCodeForToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");

        try {
            String responseBody = restClient.post()
                    .uri("https://www.strava.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);

            StravaToken token = tokenRepository.findFirstByOrderByIdAsc().orElse(new StravaToken());
            token.setAccessToken(root.path("access_token").asText());
            token.setRefreshToken(root.path("refresh_token").asText());
            token.setExpiresAt(root.path("expires_at").asLong());

            JsonNode athlete = root.path("athlete");
            token.setAthleteId(athlete.path("id").asLong());
            String firstName = athlete.path("firstname").asText("");
            String lastName = athlete.path("lastname").asText("");
            token.setAthleteName((firstName + " " + lastName).trim());
            token.setAthleteCity(athlete.path("city").asText(null));
            token.setProfileMedium(athlete.path("profile_medium").asText(null));

            tokenRepository.save(token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange Strava code for token", e);
        }
    }

    private StravaToken refreshTokenIfExpired(StravaToken token) {
        long nowPlusBuffer = System.currentTimeMillis() / 1000 + 300;
        if (token.getExpiresAt() != null && token.getExpiresAt() > nowPlusBuffer) {
            return token;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", token.getRefreshToken());

        try {
            String responseBody = restClient.post()
                    .uri("https://www.strava.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            token.setAccessToken(root.path("access_token").asText());
            token.setRefreshToken(root.path("refresh_token").asText());
            token.setExpiresAt(root.path("expires_at").asLong());
            return tokenRepository.save(token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh Strava token", e);
        }
    }

    public StravaStatusDto getStatus() {
        Optional<StravaToken> tokenOpt = tokenRepository.findFirstByOrderByIdAsc();
        if (tokenOpt.isEmpty()) {
            return new StravaStatusDto(false, null, null, null);
        }
        StravaToken token = tokenOpt.get();
        return new StravaStatusDto(true, token.getAthleteName(), token.getAthleteCity(), token.getProfileMedium());
    }

    public List<StravaActivityDto> getActivities(LocalDate start, LocalDate end) {
        Optional<StravaToken> tokenOpt = tokenRepository.findFirstByOrderByIdAsc();
        if (tokenOpt.isEmpty()) {
            return List.of();
        }

        StravaToken token = refreshTokenIfExpired(tokenOpt.get());
        long after = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long before = end.atStartOfDay(ZoneOffset.UTC).toEpochSecond() + 86399;

        try {
            String responseBody = restClient.get()
                    .uri("https://www.strava.com/api/v3/athlete/activities?after=" + after + "&before=" + before + "&per_page=50")
                    .header("Authorization", "Bearer " + token.getAccessToken())
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(responseBody, new TypeReference<List<StravaActivityDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Strava activities", e);
        }
    }

    public void disconnect() {
        tokenRepository.deleteAll();
    }
}
