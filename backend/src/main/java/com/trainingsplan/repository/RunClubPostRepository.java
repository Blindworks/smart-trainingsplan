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
}
