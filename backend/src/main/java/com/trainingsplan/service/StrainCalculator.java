package com.trainingsplan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Computes training strain (load) from heart rate zone times.
 *
 * <p>Zone weights (v1): Z1=1, Z2=2, Z3=3, Z4=5, Z5=8<br>
 * rawLoad = Σ (z_i_min × weight_i)<br>
 * strain21 = 21 × (1 − exp(−rawLoad / k)), default k=120
 *
 * <p>Configure via {@code strain.k} in application.properties.
 */
@Service
public class StrainCalculator {

    /** Zone weights indexed by zone ordinal (Z1=0 … Z5=4). */
    private static final double[] ZONE_WEIGHTS = {1.0, 2.0, 3.0, 5.0, 8.0};

    /**
     * Compression constant for the exponential scaling.
     * A rawLoad of k maps to roughly 63% of 21 (≈ 13.3).
     * Field initializer ensures value is 120.0 when used outside Spring context (tests).
     */
    @Value("${strain.k:120.0}")
    private double k = 120.0;

    /**
     * Computes the raw load from zone times.
     *
     * @param z1Min minutes in Z1
     * @param z2Min minutes in Z2
     * @param z3Min minutes in Z3
     * @param z4Min minutes in Z4
     * @param z5Min minutes in Z5
     * @return weighted sum (rawLoad)
     */
    public double rawLoad(double z1Min, double z2Min, double z3Min, double z4Min, double z5Min) {
        return z1Min * ZONE_WEIGHTS[0]
             + z2Min * ZONE_WEIGHTS[1]
             + z3Min * ZONE_WEIGHTS[2]
             + z4Min * ZONE_WEIGHTS[3]
             + z5Min * ZONE_WEIGHTS[4];
    }

    /**
     * Compresses rawLoad to the [0, 21) scale using a monotone exponential formula.
     *
     * @param rawLoad the value returned by {@link #rawLoad}
     * @return strain value in [0, 21)
     */
    public double strain21(double rawLoad) {
        return 21.0 * (1.0 - Math.exp(-rawLoad / k));
    }
}
