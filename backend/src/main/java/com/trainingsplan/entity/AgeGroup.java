package com.trainingsplan.entity;

/** Ironman-style 5-year age groups. Stable {@code key} is the public API/UI value. */
public enum AgeGroup {
    U18("U18", 0, 17),
    AG_18_24("18-24", 18, 24),
    AG_25_29("25-29", 25, 29),
    AG_30_34("30-34", 30, 34),
    AG_35_39("35-39", 35, 39),
    AG_40_44("40-44", 40, 44),
    AG_45_49("45-49", 45, 49),
    AG_50_54("50-54", 50, 54),
    AG_55_59("55-59", 55, 59),
    AG_60_64("60-64", 60, 64),
    AG_65_69("65-69", 65, 69),
    AG_70_PLUS("70+", 70, Integer.MAX_VALUE);

    private final String key;
    private final int minAge;
    private final int maxAge;

    AgeGroup(String key, int minAge, int maxAge) {
        this.key = key;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public String getKey() {
        return key;
    }

    /** Bucket for an age, or null if the age is negative/implausible. */
    public static AgeGroup forAge(int age) {
        if (age < 0) {
            return null;
        }
        for (AgeGroup g : values()) {
            if (age >= g.minAge && age <= g.maxAge) {
                return g;
            }
        }
        return null;
    }

    public static AgeGroup fromBirthYear(int birthYear, int referenceYear) {
        return forAge(referenceYear - birthYear);
    }

    /** Resolve a stable API key (e.g. "30-34") to a bucket; null/unknown -> null. */
    public static AgeGroup fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (AgeGroup g : values()) {
            if (g.key.equals(key)) {
                return g;
            }
        }
        return null;
    }
}
