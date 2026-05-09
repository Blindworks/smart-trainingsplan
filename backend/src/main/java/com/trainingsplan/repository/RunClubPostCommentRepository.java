package com.trainingsplan.repository;

import com.trainingsplan.entity.RunClubPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RunClubPostCommentRepository extends JpaRepository<RunClubPostComment, Long> {

    @Query("SELECT c FROM RunClubPostComment c WHERE c.post.id = :postId AND c.deletedAt IS NULL " +
           "ORDER BY c.createdAt ASC")
    List<RunClubPostComment> findActiveByPostId(@Param("postId") Long postId);

    long countByPostIdAndDeletedAtIsNull(Long postId);
}
