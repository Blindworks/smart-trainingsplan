package com.trainingsplan.repository;

import com.trainingsplan.entity.RunClubPostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunClubPostImageRepository extends JpaRepository<RunClubPostImage, Long> {

    List<RunClubPostImage> findByPostIdOrderBySortOrderAsc(Long postId);
}
