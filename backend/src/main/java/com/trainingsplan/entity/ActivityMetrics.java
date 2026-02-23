package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Stores computed heart rate zone metrics for a completed training activity.
 * One-to-one with {@link CompletedTraining}.
 *
 * All z*Min fields are NULL when zones could not be computed
 * (e.g. no hrMax configured, or HR stream missing).
 */
@Entity
@Table(name = "activity_metrics")
public class ActivityMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_training_id", nullable = false, unique = true)
    private CompletedTraining completedTraining;

    /** True when zone calculation was not possible (missing hrMax or stream). */
    @Column(name = "zones_unknown", nullable = false)
    private Boolean zonesUnknown = false;

    @Column(name = "z1_min")
    private Double z1Min;

    @Column(name = "z2_min")
    private Double z2Min;

    @Column(name = "z3_min")
    private Double z3Min;

    @Column(name = "z4_min")
    private Double z4Min;

    @Column(name = "z5_min")
    private Double z5Min;

    /**
     * Fraction of total activity duration with valid HR data (0.0–1.0).
     * NULL when zones_unknown = true.
     */
    @Column(name = "hr_data_coverage")
    private Double hrDataCoverage;

    /** Weighted zone load: Z1*1 + Z2*2 + Z3*3 + Z4*5 + Z5*8. NULL when zones_unknown = true. */
    @Column(name = "raw_load")
    private Double rawLoad;

    /** Exponentially compressed load on [0, 21) scale. NULL when zones_unknown = true. */
    @Column(name = "strain21")
    private Double strain21;

    /** Bannister TRIMP value. NULL when hrRest or hrMax is not configured. */
    @Column(name = "trimp")
    private Double trimp;

    /**
     * Reliability of the TRIMP value: {@code LOW} when HR coverage &lt; 60%, {@code OK} otherwise.
     * NULL when TRIMP could not be computed.
     */
    @Column(name = "trimp_quality", length = 10)
    private String trimpQuality;

    public ActivityMetrics() {}

    // Getters and setters

    public Long getId() { return id; }

    public CompletedTraining getCompletedTraining() { return completedTraining; }
    public void setCompletedTraining(CompletedTraining completedTraining) {
        this.completedTraining = completedTraining;
    }

    public Boolean getZonesUnknown() { return zonesUnknown; }
    public void setZonesUnknown(Boolean zonesUnknown) { this.zonesUnknown = zonesUnknown; }

    public Double getZ1Min() { return z1Min; }
    public void setZ1Min(Double z1Min) { this.z1Min = z1Min; }

    public Double getZ2Min() { return z2Min; }
    public void setZ2Min(Double z2Min) { this.z2Min = z2Min; }

    public Double getZ3Min() { return z3Min; }
    public void setZ3Min(Double z3Min) { this.z3Min = z3Min; }

    public Double getZ4Min() { return z4Min; }
    public void setZ4Min(Double z4Min) { this.z4Min = z4Min; }

    public Double getZ5Min() { return z5Min; }
    public void setZ5Min(Double z5Min) { this.z5Min = z5Min; }

    public Double getHrDataCoverage() { return hrDataCoverage; }
    public void setHrDataCoverage(Double hrDataCoverage) { this.hrDataCoverage = hrDataCoverage; }

    public Double getRawLoad() { return rawLoad; }
    public void setRawLoad(Double rawLoad) { this.rawLoad = rawLoad; }

    public Double getStrain21() { return strain21; }
    public void setStrain21(Double strain21) { this.strain21 = strain21; }

    public Double getTrimp() { return trimp; }
    public void setTrimp(Double trimp) { this.trimp = trimp; }

    public String getTrimpQuality() { return trimpQuality; }
    public void setTrimpQuality(String trimpQuality) { this.trimpQuality = trimpQuality; }
}
