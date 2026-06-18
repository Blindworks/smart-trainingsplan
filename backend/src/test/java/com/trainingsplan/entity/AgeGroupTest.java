package com.trainingsplan.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgeGroupTest {

    @Test
    void forAge_mapsBoundariesToCorrectBucket() {
        assertEquals(AgeGroup.U18, AgeGroup.forAge(17));
        assertEquals(AgeGroup.AG_18_24, AgeGroup.forAge(18));
        assertEquals(AgeGroup.AG_18_24, AgeGroup.forAge(24));
        assertEquals(AgeGroup.AG_25_29, AgeGroup.forAge(25));
        assertEquals(AgeGroup.AG_65_69, AgeGroup.forAge(69));
        assertEquals(AgeGroup.AG_70_PLUS, AgeGroup.forAge(70));
        assertEquals(AgeGroup.AG_70_PLUS, AgeGroup.forAge(95));
    }

    @Test
    void fromBirthYear_usesReferenceYear() {
        assertEquals(AgeGroup.AG_40_44, AgeGroup.fromBirthYear(1986, 2026));
        assertEquals(AgeGroup.U18, AgeGroup.fromBirthYear(2009, 2026));
    }

    @Test
    void fromKey_resolvesStableApiKeys() {
        assertEquals(AgeGroup.AG_30_34, AgeGroup.fromKey("30-34"));
        assertEquals(AgeGroup.AG_70_PLUS, AgeGroup.fromKey("70+"));
        assertEquals(AgeGroup.U18, AgeGroup.fromKey("U18"));
        assertNull(AgeGroup.fromKey("nonsense"));
        assertNull(AgeGroup.fromKey(null));
    }

    @Test
    void getKey_returnsStableApiKey() {
        assertEquals("30-34", AgeGroup.AG_30_34.getKey());
        assertEquals("70+", AgeGroup.AG_70_PLUS.getKey());
    }
}
