package com.trainingsplan.repository;

import com.trainingsplan.entity.RunClubPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RunClubPostRepository extends JpaRepository<RunClubPost, Long> {

    @Query("SELECT p FROM RunClubPost p WHERE p.club.id = :clubId AND p.deletedAt IS NULL " +
           "ORDER BY p.createdAt DESC")
    Page<RunClubPost> findActiveByClubId(@Param("clubId") Long clubId, Pageable pageable);

    long countByClubIdAndDeletedAtIsNull(Long clubId);

    /**
     * Aggregated feed for the News Hub: all non-deleted posts from clubs where the user
     * holds an ACTIVE membership, newest first.
     */
    @Query("SELECT p FROM RunClubPost p WHERE p.deletedAt IS NULL AND p.club.id IN " +
           "(SELECT m.club.id FROM RunClubMembership m WHERE m.user.id = :userId " +
           "AND m.status = com.trainingsplan.entity.RunClubMembershipStatus.ACTIVE) " +
           "ORDER BY p.createdAt DESC")
    Page<RunClubPost> findFeedForUser(@Param("userId") Long userId, Pageable pageable);
}
