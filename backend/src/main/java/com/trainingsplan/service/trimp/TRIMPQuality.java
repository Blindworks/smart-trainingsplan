package com.trainingsplan.service.trimp;

/**
 * Indicates the reliability of a computed TRIMP value based on HR data coverage.
 */
public enum TRIMPQuality {
    /** HR coverage &lt; 60% of activity duration — TRIMP value may be unreliable. */
    LOW,
    /** HR coverage ≥ 60% — TRIMP value is considered reliable. */
    OK
}
