package com.trainingsplan.repository;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findTop50ByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadAtIsNull(User user);

    List<UserNotification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);

    boolean existsByUserAndTypeAndReferenceId(User user, UserNotification.Type type, Long referenceId);
}
