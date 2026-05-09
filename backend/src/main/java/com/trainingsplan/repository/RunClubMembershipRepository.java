package com.trainingsplan.repository;

import com.trainingsplan.entity.RunClub;
import com.trainingsplan.entity.RunClubMemberRole;
import com.trainingsplan.entity.RunClubMembership;
import com.trainingsplan.entity.RunClubMembershipStatus;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RunClubMembershipRepository extends JpaRepository<RunClubMembership, Long> {

    Optional<RunClubMembership> findByClubAndUser(RunClub club, User user);

    Optional<RunClubMembership> findByClubIdAndUserId(Long clubId, Long userId);

    List<RunClubMembership> findByClubAndStatus(RunClub club, RunClubMembershipStatus status);

    List<RunClubMembership> findByClubIdAndStatus(Long clubId, RunClubMembershipStatus status);

    List<RunClubMembership> findByUserAndStatus(User user, RunClubMembershipStatus status);

    List<RunClubMembership> findByUserId(Long userId);

    long countByClubAndStatus(RunClub club, RunClubMembershipStatus status);

    long countByClubIdAndStatus(Long clubId, RunClubMembershipStatus status);

    long countByClubAndStatusAndRole(RunClub club, RunClubMembershipStatus status, RunClubMemberRole role);

    long countByClubIdAndStatusAndRole(Long clubId, RunClubMembershipStatus status, RunClubMemberRole role);

    boolean existsByClubAndUserAndStatus(RunClub club, User user, RunClubMembershipStatus status);

    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, RunClubMembershipStatus status);

    /** Returns all active member IDs for stats aggregation. */
    List<RunClubMembership> findByClubIdAndStatusAndRole(Long clubId, RunClubMembershipStatus status, RunClubMemberRole role);
}
