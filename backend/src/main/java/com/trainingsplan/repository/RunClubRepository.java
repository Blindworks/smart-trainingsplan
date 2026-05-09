package com.trainingsplan.repository;

import com.trainingsplan.entity.RunClub;
import com.trainingsplan.entity.RunClubStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunClubRepository extends JpaRepository<RunClub, Long> {

    List<RunClub> findByStatus(RunClubStatus status, Pageable pageable);

    List<RunClub> findByStatus(RunClubStatus status);

    Optional<RunClub> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("SELECT rc FROM RunClub rc WHERE rc.status = 'APPROVED' " +
           "AND LOWER(rc.locationCity) = LOWER(:city)")
    List<RunClub> findApprovedByLocationCityIgnoreCase(@Param("city") String city);

    /**
     * Haversine distance filter — returns approved clubs within the given radius.
     * Pre-filtered by a bounding box for efficiency, then the caller applies exact distance.
     */
    @Query("SELECT rc FROM RunClub rc WHERE rc.status = 'APPROVED' " +
           "AND rc.latitude IS NOT NULL AND rc.longitude IS NOT NULL " +
           "AND rc.latitude BETWEEN :latMin AND :latMax " +
           "AND rc.longitude BETWEEN :lngMin AND :lngMax")
    List<RunClub> findApprovedInBoundingBox(
            @Param("latMin") double latMin,
            @Param("latMax") double latMax,
            @Param("lngMin") double lngMin,
            @Param("lngMax") double lngMax);

    @Query("SELECT DISTINCT rc FROM RunClub rc JOIN rc.meetingDays d " +
           "WHERE rc.status = 'APPROVED' AND d = :day")
    List<RunClub> findApprovedByMeetingDay(@Param("day") DayOfWeek day);

    @Query("SELECT rc FROM RunClub rc WHERE rc.status = 'APPROVED' " +
           "AND LOWER(rc.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<RunClub> findApprovedByNameContaining(@Param("q") String q);

    @Query("SELECT rc FROM RunClub rc WHERE rc.status = 'APPROVED' " +
           "AND rc.latitude IS NOT NULL AND rc.longitude IS NOT NULL")
    List<RunClub> findAllApprovedForMap();
}
