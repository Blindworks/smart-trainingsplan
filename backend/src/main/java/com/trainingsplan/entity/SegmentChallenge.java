package com.trainingsplan.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "segment_challenges")
public class SegmentChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "start_lng", nullable = false)
    private Double startLng;

    @Column(name = "end_lat", nullable = false)
    private Double endLat;

    @Column(name = "end_lng", nullable = false)
    private Double endLng;

    @Column(name = "distance_m")
    private Double distanceM;

    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "avg_grade_pct")
    private Double avgGradePct;

    @Column(name = "max_grade_pct")
    private Double maxGradePct;

    @Column(name = "polyline_json", columnDefinition = "LONGTEXT")
    private String polylineJson;

    @Column(name = "terrain_asset_ref", length = 255)
    private String terrainAssetRef;

    @Column(name = "bounding_box_json", length = 500)
    private String boundingBoxJson;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SegmentChallenge() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public Double getStartLat() { return startLat; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }
    public Double getStartLng() { return startLng; }
    public void setStartLng(Double startLng) { this.startLng = startLng; }
    public Double getEndLat() { return endLat; }
    public void setEndLat(Double endLat) { this.endLat = endLat; }
    public Double getEndLng() { return endLng; }
    public void setEndLng(Double endLng) { this.endLng = endLng; }
    public Double getDistanceM() { return distanceM; }
    public void setDistanceM(Double distanceM) { this.distanceM = distanceM; }
    public Integer getElevationGainM() { return elevationGainM; }
    public void setElevationGainM(Integer elevationGainM) { this.elevationGainM = elevationGainM; }
    public Double getAvgGradePct() { return avgGradePct; }
    public void setAvgGradePct(Double avgGradePct) { this.avgGradePct = avgGradePct; }
    public Double getMaxGradePct() { return maxGradePct; }
    public void setMaxGradePct(Double maxGradePct) { this.maxGradePct = maxGradePct; }
    public String getPolylineJson() { return polylineJson; }
    public void setPolylineJson(String polylineJson) { this.polylineJson = polylineJson; }
    public String getTerrainAssetRef() { return terrainAssetRef; }
    public void setTerrainAssetRef(String terrainAssetRef) { this.terrainAssetRef = terrainAssetRef; }
    public String getBoundingBoxJson() { return boundingBoxJson; }
    public void setBoundingBoxJson(String boundingBoxJson) { this.boundingBoxJson = boundingBoxJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
