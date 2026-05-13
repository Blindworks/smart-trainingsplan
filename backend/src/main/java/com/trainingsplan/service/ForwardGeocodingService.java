package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Forward-geocoding (address → lat/lng) via Nominatim. Counterpart to
 * {@link ReverseGeocodingService}. Used by the LADV importer to attach
 * coordinates to staged events whose JSON only carries an address string.
 *
 * Nominatim usage policy: 1 req/s, meaningful User-Agent.
 */
@Service
public class ForwardGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(ForwardGeocodingService.class);
    private static final String NOMINATIM_URL_DE =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=de&q=";
    private static final String NOMINATIM_URL_BASE =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=";
    private static final String NOMINATIM_URL_COUNTRY =
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes={cc}&q=";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    public ForwardGeocodingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record LatLng(double lat, double lng) {}

    /** Returns the best Nominatim hit for {@code query} (defaults to DE), or null on miss/failure. */
    public LatLng geocode(String query) {
        return geocode(query, "de");
    }

    /**
     * Returns the best Nominatim hit for {@code query} restricted to {@code countryCode}
     * (ISO 3166-1 alpha-2, case-insensitive). When {@code countryCode} is null or blank,
     * the search is global. Returns null on miss or failure.
     */
    public LatLng geocode(String query, String countryCode) {
        if (query == null || query.isBlank()) return null;
        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url;
            if (countryCode == null || countryCode.isBlank()) {
                url = NOMINATIM_URL_BASE + encoded;
            } else if ("de".equalsIgnoreCase(countryCode.trim())) {
                url = NOMINATIM_URL_DE + encoded;
            } else {
                url = NOMINATIM_URL_COUNTRY.replace("{cc}", countryCode.trim().toLowerCase()) + encoded;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "PACR/1.0 (pacr.app)")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Nominatim search returned {} for '{}'", response.statusCode(), query);
                return null;
            }
            JsonNode arr = objectMapper.readTree(response.body());
            if (!arr.isArray() || arr.isEmpty()) return null;
            JsonNode hit = arr.get(0);
            JsonNode lat = hit.get("lat");
            JsonNode lon = hit.get("lon");
            if (lat == null || lon == null) return null;
            return new LatLng(Double.parseDouble(lat.asText()), Double.parseDouble(lon.asText()));
        } catch (Exception e) {
            log.warn("Forward geocoding failed for '{}': {}", query, e.getMessage());
            return null;
        }
    }
}
