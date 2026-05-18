package com.trainingsplan.repository;

import com.trainingsplan.entity.BuddyRun;
import com.trainingsplan.entity.BuddyRunParticipant;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuddyRunParticipantRepository extends JpaRepository<BuddyRunParticipant, Long> {

    Optional<BuddyRunParticipant> findByBuddyRunAndUser(BuddyRun buddyRun, User user);

    List<BuddyRunParticipant> findByBuddyRun(BuddyRun buddyRun);

    long countByBuddyRunAndStatus(BuddyRun buddyRun, BuddyRunParticipant.Status status);
}
