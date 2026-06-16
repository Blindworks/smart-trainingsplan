package com.trainingsplan.repository;

import com.trainingsplan.entity.SegmentChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SegmentChallengeRepository extends JpaRepository<SegmentChallenge, Long> {
    Optional<SegmentChallenge> findBySlug(String slug);
}
