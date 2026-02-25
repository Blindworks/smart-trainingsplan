package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.dto.StravaStatusDto;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.StravaToken;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.StravaTokenRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StravaService {

    private static final Logger log = LoggerFactory.getLogger(StravaService.class);

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
    private final CompletedTrainingRepository completedTrainingRepository;
    private final ActivityMetricsService activityMetricsService;
    private final ActivityMetricsRepository activityMetricsRepository;
    private final SecurityUtils securityUtils;
    private final UserProfileValidationService userProfileValidationService;
    private final RestClient restClient;

    public StravaService(StravaTokenRepository tokenRepository, ObjectMapper objectMapper,
                         CompletedTrainingRepository completedTrainingRepository,
                         ActivityMetricsService activityMetricsService,
                         ActivityMetricsRepository activityMetricsRepository,
                         SecurityUtils securityUtils,
                         UserProfileValidationService userProfileValidationService) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.completedTrainingRepository = completedTrainingRepository;
        this.activityMetricsService = activityMetricsService;
        this.activityMetricsRepository = activityMetricsRepository;
        this.securityUtils = securityUtils;
        this.userProfileValidationService = userProfileValidationService;
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

            List<StravaActivityDto> activities = objectMapper.readValue(responseBody, new TypeReference<List<StravaActivityDto>>() {});
            User currentUser = securityUtils.getCurrentUser();
            syncActivitiesToDb(activities, token.getAccessToken(), currentUser);
            return activities;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Strava activities", e);
        }
    }

    private void syncActivitiesToDb(List<StravaActivityDto> activities, String accessToken, User user) {
        for (StravaActivityDto dto : activities) {
            if (dto.getId() != null && !completedTrainingRepository.existsByStravaActivityId(dto.getId())) {
                try {
                    CompletedTraining ct = convertStravaActivityToCompletedTraining(dto);
                    ct.setUser(user);
                    CompletedTraining saved = completedTrainingRepository.save(ct);
                    fetchStreamsAndPersistMetrics(dto.getId(), saved, accessToken, user);
                } catch (Exception e) {
                    log.warn("Failed to persist Strava activity id={}: {}", dto.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Fetches time, heartrate, velocity_smooth, and distance streams from Strava for one activity
     * and persists zone metrics + aerobic decoupling.
     * Silently skips on any error (stream may not exist for activities without HR sensor).
     */
    private void fetchStreamsAndPersistMetrics(Long stravaActivityId, CompletedTraining ct,
                                               String accessToken, User user) {
        try {
            ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
            if (!completion.complete()) {
                log.info("Skipping metric calculation for Strava activity {}. Incomplete profile: {}",
                        stravaActivityId, completion.missingFields());
                return;
            }

            String url = "https://www.strava.com/api/v3/activities/" + stravaActivityId
                    + "/streams?keys=time,heartrate,velocity_smooth,distance&key_by_type=true";
            String body = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root     = objectMapper.readTree(body);
            JsonNode timeData = root.path("time").path("data");
            JsonNode hrData   = root.path("heartrate").path("data");
            JsonNode velData  = root.path("velocity_smooth").path("data");
            JsonNode distData = root.path("distance").path("data");

            if (timeData.isMissingNode() || !timeData.isArray() || timeData.isEmpty()) return;

            List<Integer> timeSeconds = new ArrayList<>(timeData.size());
            for (JsonNode t : timeData) timeSeconds.add(t.intValue());

            List<Integer> heartRates = new ArrayList<>(timeSeconds.size());
            if (!hrData.isMissingNode() && hrData.isArray() && hrData.size() == timeSeconds.size()) {
                for (JsonNode hr : hrData) heartRates.add(hr.isNull() ? null : hr.intValue());
            } else {
                for (int i = 0; i < timeSeconds.size(); i++) heartRates.add(null);
            }

            List<Double> velocities = new ArrayList<>(timeSeconds.size());
            if (!velData.isMissingNode() && velData.isArray() && velData.size() == timeSeconds.size()) {
                for (JsonNode v : velData) velocities.add(v.isNull() ? null : v.doubleValue());
            } else {
                for (int i = 0; i < timeSeconds.size(); i++) velocities.add(null);
            }

            List<Double> distances = null;
            if (!distData.isMissingNode() && distData.isArray() && distData.size() == timeSeconds.size()) {
                distances = new ArrayList<>(timeSeconds.size());
                for (JsonNode d : distData) distances.add(d.isNull() ? null : d.doubleValue());
            }

            activityMetricsService.calculateAndPersist(ct, timeSeconds, heartRates, velocities, distances, user);
        } catch (Exception e) {
            log.warn("Could not fetch/compute streams for Strava activity {}: {}", stravaActivityId, e.getMessage());
        }
    }

    /**
     * Retroactively computes zone metrics for a CompletedTraining that was synced from Strava.
     * Called on demand (e.g. from the activity dialog) for activities synced before this feature existed.
     */
    public ActivityMetrics computeMetricsForCompletedTraining(Long completedTrainingId) {
        CompletedTraining ct = completedTrainingRepository.findById(completedTrainingId)
                .orElseThrow(() -> new RuntimeException("CompletedTraining not found: " + completedTrainingId));

        if (ct.getStravaActivityId() == null) {
            throw new RuntimeException("Activity " + completedTrainingId + " is not a Strava activity");
        }

        StravaToken token = tokenRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No Strava token found"));
        token = refreshTokenIfExpired(token);

        User user = securityUtils.getCurrentUser();
        if (ct.getUser() == null && user != null) {
            ct.setUser(user);
            ct = completedTrainingRepository.save(ct);
        }
        fetchStreamsAndPersistMetrics(ct.getStravaActivityId(), ct, token.getAccessToken(), user);

        return activityMetricsRepository.findByCompletedTrainingId(completedTrainingId).orElse(null);
    }

    private CompletedTraining convertStravaActivityToCompletedTraining(StravaActivityDto dto) {
        CompletedTraining ct = new CompletedTraining();
        ct.setSource("STRAVA");
        ct.setStravaActivityId(dto.getId());
        ct.setActivityName(dto.getName());

        String sport = dto.getSportType() != null ? dto.getSportType() : dto.getType();
        ct.setSport(sport);

        // Prefer start_date_local (user timezone) over UTC to get the correct calendar date
        String dateStr = dto.getStartDateLocal() != null ? dto.getStartDateLocal() : dto.getStartDate();
        ct.setTrainingDate(LocalDate.parse(dateStr.substring(0, 10)));

        if (dto.getDistanceMeters() != null && dto.getDistanceMeters() > 0) {
            ct.setDistanceKm(dto.getDistanceMeters() / 1000.0);
        }
        if (dto.getMovingTimeSeconds() != null) {
            ct.setMovingTimeSeconds(dto.getMovingTimeSeconds());
            ct.setDurationSeconds(dto.getMovingTimeSeconds());
        }
        if (dto.getTotalElevationGain() != null && dto.getTotalElevationGain() > 0) {
            ct.setElevationGainM(dto.getTotalElevationGain().intValue());
        }
        if (dto.getAverageSpeed() != null && dto.getAverageSpeed() > 0) {
            ct.setAverageSpeedKmh(dto.getAverageSpeed() * 3.6);
            double paceSecondsPerKm = 1000.0 / dto.getAverageSpeed();
            ct.setAveragePaceSecondsPerKm((int) paceSecondsPerKm);
        }
        if (dto.getMaxSpeed() != null && dto.getMaxSpeed() > 0) {
            ct.setMaxSpeedKmh(dto.getMaxSpeed() * 3.6);
        }
        if (dto.getAverageHeartrate() != null) {
            ct.setAverageHeartRate(dto.getAverageHeartrate().intValue());
        }
        if (dto.getMaxHeartrate() != null) {
            ct.setMaxHeartRate(dto.getMaxHeartrate().intValue());
        }
        if (dto.getAverageWatts() != null) {
            ct.setAveragePowerWatts(dto.getAverageWatts().intValue());
        }

        return ct;
    }

    public void disconnect() {
        tokenRepository.deleteAll();
    }
}
