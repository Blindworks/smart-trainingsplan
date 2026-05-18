package com.trainingsplan.repository;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserBuddyPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBuddyPreferencesRepository extends JpaRepository<UserBuddyPreferences, Long> {

    Optional<UserBuddyPreferences> findByUser(User user);

    List<UserBuddyPreferences> findByBuddyDiscoverableTrue();
}
