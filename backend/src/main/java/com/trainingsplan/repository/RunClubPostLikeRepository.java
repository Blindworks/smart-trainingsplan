package com.trainingsplan.repository;

import com.trainingsplan.entity.RunClubPost;
import com.trainingsplan.entity.RunClubPostLike;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RunClubPostLikeRepository extends JpaRepository<RunClubPostLike, Long> {

    Optional<RunClubPostLike> findByPostAndUser(RunClubPost post, User user);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostAndUser(RunClubPost post, User user);
}
