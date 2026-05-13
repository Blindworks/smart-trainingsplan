package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.RunWeatherForecastDto;
import com.trainingsplan.dto.RunWeatherForecastDto.HourlyPoint;
import com.trainingsplan.dto.RunWeatherForecastDto.Verdict;
import com.trainingsplan.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Short-term run-weather forecast. Backed by Open-Meteo, which serves the DWD ICON-D2 model
 * for Germany — the same source the user requested. We aggregate the next 12 hours of hourly
 * forecast into a single GOOD/CAUTION/BAD verdict plus human-readable reasons.
 *
 * <p>Results are cached per coordinate-bucket for 30 minutes to keep external traffic low.</p>
 */
@Service
public class RunWeatherForecastService {

    private static final Logger log = LoggerFactory.getLogger(RunWeatherForecastService.class);

    private static final String FORECAST_URL_TEMPLATE =
            "https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}" +
            "&current=temperature_2m,precipitation,precipitation_probability,wind_speed_10m,weather_code" +
            "&hourly=temperature_2m,precipitation,precipitation_probability,wind_speed_10m,weather_code" +
            "&forecast_days=2&timezone=auto&models=icon_seamless";

    /** Number of hourly slots aggregated into the verdict (i.e. forecast window length). */
    private static final int WINDOW_HOURS = 12;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RunWeatherForecastService() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    private record CacheEntry(Instant cachedAt, RunWeatherForecastDto dto) {}

    /**
     * Builds a forecast for the given user. Returns {@code null} when the user has neither
     * an address nor explicit coordinates set — the caller should respond with 204 No Content
     * and prompt the user to fill in their address.
     */
    public RunWeatherForecastDto getForecastForUser(User user) {
        if (user.getLatitude() == null || user.getLongitude() == null) {
            return null;
        }
        return getForecast(user.getLatitude(), user.getLongitude(), buildLocationLabel(user));
    }

    public RunWeatherForecastDto getForecast(double latitude, double longitude, String locationLabel) {
        String cacheKey = bucketKey(latitude, longitude);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && Duration.between(cached.cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            // Return a shallow copy with an updated label so the response reflects the caller's
            // address even if the cached one was built for another user at the same bucket.
            RunWeatherForecastDto dto = cached.dto;
            if (locationLabel != null && !locationLabel.equals(dto.getLocationLabel())) {
                RunWeatherForecastDto copy = shallowCopy(dto);
                copy.setLocationLabel(locationLabel);
                return copy;
            }
            return dto;
        }

        RunWeatherForecastDto dto = fetchForecast(latitude, longitude, locationLabel);
        if (dto != null) {
            cache.put(cacheKey, new CacheEntry(Instant.now(), dto));
        }
        return dto;
    }

    private RunWeatherForecastDto fetchForecast(double lat, double lon, String locationLabel) {
        String url = FORECAST_URL_TEMPLATE
                .replace("{lat}", String.valueOf(lat))
                .replace("{lon}", String.valueOf(lon));
        try {
            String body = restClient.get().uri(url).retrieve().body(String.class);
            if (body == null) return null;
            JsonNode root = objectMapper.readTree(body);

            RunWeatherForecastDto dto = new RunWeatherForecastDto();
            dto.setLatitude(lat);
            dto.setLongitude(lon);
            dto.setLocationLabel(locationLabel);
            dto.setDataSource("DWD ICON (via Open-Meteo)");

            JsonNode current = root.path("current");
            if (!current.isMissingNode()) {
                if (current.has("temperature_2m")) dto.setCurrentTemperatureC(current.path("temperature_2m").asDouble());
                if (current.has("precipitation")) dto.setCurrentPrecipitationMm(current.path("precipitation").asDouble());
                if (current.has("precipitation_probability"))
                    dto.setCurrentPrecipitationProbabilityPct(current.path("precipitation_probability").asInt());
                if (current.has("wind_speed_10m")) dto.setCurrentWindKmh(current.path("wind_speed_10m").asDouble());
                if (current.has("weather_code")) dto.setCurrentWeatherCode(current.path("weather_code").asInt());
            }

            JsonNode hourly = root.path("hourly");
            List<HourlyPoint> points = extractHourlyWindow(hourly);
            dto.setHourly(points);

            if (!points.isEmpty()) {
                dto.setValidFrom(points.get(0).time);
                dto.setValidUntil(points.get(points.size() - 1).time);
            }
            aggregate(dto, points);
            decide(dto);
            return dto;
        } catch (Exception e) {
            log.warn("Failed to fetch run-weather forecast for {},{}: {}", lat, lon, e.getMessage());
            return null;
        }
    }

    private static List<HourlyPoint> extractHourlyWindow(JsonNode hourly) {
        List<HourlyPoint> result = new ArrayList<>();
        if (hourly.isMissingNode()) return result;
        JsonNode times = hourly.path("time");
        JsonNode temps = hourly.path("temperature_2m");
        JsonNode precs = hourly.path("precipitation");
        JsonNode probs = hourly.path("precipitation_probability");
        JsonNode winds = hourly.path("wind_speed_10m");
        JsonNode codes = hourly.path("weather_code");
        if (!times.isArray()) return result;

        LocalDateTime nowLocal = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        int startIdx = 0;
        for (int i = 0; i < times.size(); i++) {
            LocalDateTime t = LocalDateTime.parse(times.get(i).asText(), fmt);
            // Find the first hourly slot whose timestamp is >= the start of the current hour.
            if (!t.isBefore(nowLocal.withMinute(0).withSecond(0).withNano(0))) {
                startIdx = i;
                break;
            }
        }
        int endIdx = Math.min(times.size(), startIdx + WINDOW_HOURS);
        for (int i = startIdx; i < endIdx; i++) {
            HourlyPoint p = new HourlyPoint();
            p.time = LocalDateTime.parse(times.get(i).asText(), fmt);
            p.temperatureC = optionalDouble(temps, i);
            p.precipitationMm = optionalDouble(precs, i);
            p.precipitationProbabilityPct = optionalInt(probs, i);
            p.windKmh = optionalDouble(winds, i);
            p.weatherCode = optionalInt(codes, i);
            result.add(p);
        }
        return result;
    }

    private static Double optionalDouble(JsonNode arr, int i) {
        if (arr == null || !arr.isArray() || i >= arr.size() || arr.get(i).isNull()) return null;
        return arr.get(i).asDouble();
    }

    private static Integer optionalInt(JsonNode arr, int i) {
        if (arr == null || !arr.isArray() || i >= arr.size() || arr.get(i).isNull()) return null;
        return arr.get(i).asInt();
    }

    private static void aggregate(RunWeatherForecastDto dto, List<HourlyPoint> points) {
        if (points.isEmpty()) return;
        int maxProb = 0;
        double totalPrec = 0;
        double maxWind = 0;
        double minTemp = Double.POSITIVE_INFINITY;
        double maxTemp = Double.NEGATIVE_INFINITY;
        boolean thunder = false;
        LocalDateTime firstRain = null;
        for (HourlyPoint p : points) {
            if (p.precipitationProbabilityPct != null) maxProb = Math.max(maxProb, p.precipitationProbabilityPct);
            if (p.precipitationMm != null) totalPrec += p.precipitationMm;
            if (p.windKmh != null) maxWind = Math.max(maxWind, p.windKmh);
            if (p.temperatureC != null) {
                minTemp = Math.min(minTemp, p.temperatureC);
                maxTemp = Math.max(maxTemp, p.temperatureC);
            }
            if (p.weatherCode != null && isThunderstormCode(p.weatherCode)) thunder = true;
            if (firstRain == null
                    && ((p.precipitationProbabilityPct != null && p.precipitationProbabilityPct >= 50)
                        || (p.precipitationMm != null && p.precipitationMm >= 0.5))) {
                firstRain = p.time;
            }
        }
        dto.setMaxPrecipitationProbabilityPct(maxProb);
        dto.setTotalPrecipitationMm(round1(totalPrec));
        dto.setMaxWindKmh(round1(maxWind));
        dto.setMinTemperatureC(Double.isFinite(minTemp) ? round1(minTemp) : null);
        dto.setMaxTemperatureC(Double.isFinite(maxTemp) ? round1(maxTemp) : null);
        dto.setThunderstormExpected(thunder);
        dto.setFirstRainAt(firstRain);
    }

    /** WMO weather codes for thunderstorm groups. */
    private static boolean isThunderstormCode(int code) {
        return code == 95 || code == 96 || code == 99;
    }

    private static void decide(RunWeatherForecastDto dto) {
        List<String> reasons = new ArrayList<>();
        Verdict verdict = Verdict.GOOD;

        Boolean thunder = dto.getThunderstormExpected();
        if (thunder != null && thunder) {
            reasons.add("WEATHER.REASON_THUNDERSTORM");
            verdict = Verdict.BAD;
        }

        Integer maxProb = dto.getMaxPrecipitationProbabilityPct();
        Double totalPrec = dto.getTotalPrecipitationMm();
        if (totalPrec != null && totalPrec >= 5.0) {
            reasons.add("WEATHER.REASON_HEAVY_RAIN");
            verdict = worst(verdict, Verdict.BAD);
        } else if ((maxProb != null && maxProb >= 70) || (totalPrec != null && totalPrec >= 1.0)) {
            reasons.add("WEATHER.REASON_RAIN_LIKELY");
            verdict = worst(verdict, Verdict.CAUTION);
        } else if (maxProb != null && maxProb >= 40) {
            reasons.add("WEATHER.REASON_RAIN_POSSIBLE");
            verdict = worst(verdict, Verdict.CAUTION);
        }

        Double maxWind = dto.getMaxWindKmh();
        if (maxWind != null && maxWind >= 60) {
            reasons.add("WEATHER.REASON_STORM_WIND");
            verdict = worst(verdict, Verdict.BAD);
        } else if (maxWind != null && maxWind >= 40) {
            reasons.add("WEATHER.REASON_STRONG_WIND");
            verdict = worst(verdict, Verdict.CAUTION);
        }

        Double maxTemp = dto.getMaxTemperatureC();
        Double minTemp = dto.getMinTemperatureC();
        if (maxTemp != null && maxTemp >= 32) {
            reasons.add("WEATHER.REASON_VERY_HOT");
            verdict = worst(verdict, Verdict.BAD);
        } else if (maxTemp != null && maxTemp >= 28) {
            reasons.add("WEATHER.REASON_HOT");
            verdict = worst(verdict, Verdict.CAUTION);
        }
        if (minTemp != null && minTemp <= -10) {
            reasons.add("WEATHER.REASON_VERY_COLD");
            verdict = worst(verdict, Verdict.BAD);
        } else if (minTemp != null && minTemp <= 0) {
            reasons.add("WEATHER.REASON_FREEZING");
            verdict = worst(verdict, Verdict.CAUTION);
        }

        if (reasons.isEmpty()) {
            reasons.add("WEATHER.REASON_ALL_CLEAR");
        }
        dto.setVerdict(verdict);
        dto.setReasons(reasons);
    }

    private static Verdict worst(Verdict a, Verdict b) {
        return rank(a) >= rank(b) ? a : b;
    }

    private static int rank(Verdict v) {
        return switch (v) {
            case GOOD    -> 0;
            case CAUTION -> 1;
            case BAD     -> 2;
            case UNKNOWN -> -1;
        };
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Bucket coordinates to ~1.1 km so nearby users share a cache entry. */
    private static String bucketKey(double lat, double lon) {
        return String.format(java.util.Locale.ROOT, "%.2f,%.2f", lat, lon);
    }

    private static String buildLocationLabel(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getAddressPostalCode() != null && !user.getAddressPostalCode().isBlank()) {
            sb.append(user.getAddressPostalCode());
        }
        if (user.getAddressCity() != null && !user.getAddressCity().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(user.getAddressCity());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static RunWeatherForecastDto shallowCopy(RunWeatherForecastDto src) {
        RunWeatherForecastDto c = new RunWeatherForecastDto();
        c.setVerdict(src.getVerdict());
        c.setLocationLabel(src.getLocationLabel());
        c.setLatitude(src.getLatitude());
        c.setLongitude(src.getLongitude());
        c.setValidFrom(src.getValidFrom());
        c.setValidUntil(src.getValidUntil());
        c.setDataSource(src.getDataSource());
        c.setCurrentTemperatureC(src.getCurrentTemperatureC());
        c.setCurrentPrecipitationProbabilityPct(src.getCurrentPrecipitationProbabilityPct());
        c.setCurrentPrecipitationMm(src.getCurrentPrecipitationMm());
        c.setCurrentWindKmh(src.getCurrentWindKmh());
        c.setCurrentWeatherCode(src.getCurrentWeatherCode());
        c.setMaxPrecipitationProbabilityPct(src.getMaxPrecipitationProbabilityPct());
        c.setTotalPrecipitationMm(src.getTotalPrecipitationMm());
        c.setMaxWindKmh(src.getMaxWindKmh());
        c.setMinTemperatureC(src.getMinTemperatureC());
        c.setMaxTemperatureC(src.getMaxTemperatureC());
        c.setThunderstormExpected(src.getThunderstormExpected());
        c.setFirstRainAt(src.getFirstRainAt());
        c.setHourly(src.getHourly());
        c.setReasons(src.getReasons());
        return c;
    }
}
