package com.trainingsplan.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Short-term weather forecast tailored to deciding whether the user should head out for a run.
 *
 * <p>Backed by the DWD ICON model (served via Open-Meteo). The {@link #verdict} aggregates
 * the next ~12 hours of hourly forecast into a single "GOOD / CAUTION / BAD" recommendation.
 * Detailed reasons (rain expected at 17:00, thunderstorm warning, strong wind, …) are listed
 * in {@link #reasons} so the dashboard can render them as readable bullet points.</p>
 */
public class RunWeatherForecastDto {

    /** Overall run-friendliness verdict for the next ~12 hours. */
    public enum Verdict { GOOD, CAUTION, BAD, UNKNOWN }

    private Verdict verdict;
    private String locationLabel;
    private Double latitude;
    private Double longitude;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String dataSource;

    /** Current ("now") snapshot. */
    private Double currentTemperatureC;
    private Integer currentPrecipitationProbabilityPct;
    private Double currentPrecipitationMm;
    private Double currentWindKmh;
    private Integer currentWeatherCode;

    /** Aggregates over the forecast window. */
    private Integer maxPrecipitationProbabilityPct;
    private Double  totalPrecipitationMm;
    private Double  maxWindKmh;
    private Double  minTemperatureC;
    private Double  maxTemperatureC;
    private Boolean thunderstormExpected;

    /** First hour (LocalDateTime) at which precipitation probability ≥ 50% within the window, or null. */
    private LocalDateTime firstRainAt;

    private List<HourlyPoint> hourly;
    private List<String> reasons;

    public RunWeatherForecastDto() {}

    public static class HourlyPoint {
        public LocalDateTime time;
        public Double temperatureC;
        public Integer precipitationProbabilityPct;
        public Double precipitationMm;
        public Double windKmh;
        public Integer weatherCode;
    }

    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public Double getCurrentTemperatureC() { return currentTemperatureC; }
    public void setCurrentTemperatureC(Double v) { this.currentTemperatureC = v; }

    public Integer getCurrentPrecipitationProbabilityPct() { return currentPrecipitationProbabilityPct; }
    public void setCurrentPrecipitationProbabilityPct(Integer v) { this.currentPrecipitationProbabilityPct = v; }

    public Double getCurrentPrecipitationMm() { return currentPrecipitationMm; }
    public void setCurrentPrecipitationMm(Double v) { this.currentPrecipitationMm = v; }

    public Double getCurrentWindKmh() { return currentWindKmh; }
    public void setCurrentWindKmh(Double v) { this.currentWindKmh = v; }

    public Integer getCurrentWeatherCode() { return currentWeatherCode; }
    public void setCurrentWeatherCode(Integer v) { this.currentWeatherCode = v; }

    public Integer getMaxPrecipitationProbabilityPct() { return maxPrecipitationProbabilityPct; }
    public void setMaxPrecipitationProbabilityPct(Integer v) { this.maxPrecipitationProbabilityPct = v; }

    public Double getTotalPrecipitationMm() { return totalPrecipitationMm; }
    public void setTotalPrecipitationMm(Double v) { this.totalPrecipitationMm = v; }

    public Double getMaxWindKmh() { return maxWindKmh; }
    public void setMaxWindKmh(Double v) { this.maxWindKmh = v; }

    public Double getMinTemperatureC() { return minTemperatureC; }
    public void setMinTemperatureC(Double v) { this.minTemperatureC = v; }

    public Double getMaxTemperatureC() { return maxTemperatureC; }
    public void setMaxTemperatureC(Double v) { this.maxTemperatureC = v; }

    public Boolean getThunderstormExpected() { return thunderstormExpected; }
    public void setThunderstormExpected(Boolean v) { this.thunderstormExpected = v; }

    public LocalDateTime getFirstRainAt() { return firstRainAt; }
    public void setFirstRainAt(LocalDateTime v) { this.firstRainAt = v; }

    public List<HourlyPoint> getHourly() { return hourly; }
    public void setHourly(List<HourlyPoint> hourly) { this.hourly = hourly; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
