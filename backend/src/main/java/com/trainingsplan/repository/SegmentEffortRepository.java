package com.trainingsplan.repository;

import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.entity.EffortStatus;
import com.trainingsplan.entity.SegmentEffort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SegmentEffortRepository extends JpaRepository<SegmentEffort, Long> {

    List<SegmentEffort> findByChallengeIdAndActivityTypeAndStatusOrderByElapsedSecondsAsc(
            Long challengeId, ActivityType activityType, EffortStatus status);

    long countByChallengeIdAndActivityTypeAndStatus(
            Long challengeId, ActivityType activityType, EffortStatus status);

    long countByChallengeIdAndIpHashAndCreatedAtAfter(
            Long challengeId, String ipHash, LocalDateTime after);
}
