package com.trainingsplan.service;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotification;
import com.trainingsplan.repository.UserNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final UserNotificationRepository repository;

    public NotificationService(UserNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserNotification notify(User user, UserNotification.Type type, String title, String message,
                                   String linkPath, Long referenceId) {
        UserNotification n = new UserNotification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLinkPath(linkPath);
        n.setReferenceId(referenceId);
        return repository.save(n);
    }

    public List<UserNotification> recentFor(User user) {
        return repository.findTop50ByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return repository.countByUserAndReadAtIsNull(user);
    }

    @Transactional
    public boolean markRead(Long notificationId, User user) {
        return repository.findById(notificationId)
                .filter(n -> n.getUser() != null && n.getUser().getId().equals(user.getId()))
                .map(n -> {
                    if (n.getReadAt() == null) {
                        n.setReadAt(LocalDateTime.now());
                        repository.save(n);
                    }
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void markAllRead(User user) {
        List<UserNotification> unread = repository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(user);
        LocalDateTime now = LocalDateTime.now();
        for (UserNotification n : unread) {
            n.setReadAt(now);
        }
        repository.saveAll(unread);
    }

    public boolean alreadyNotified(User user, UserNotification.Type type, Long referenceId) {
        return repository.existsByUserAndTypeAndReferenceId(user, type, referenceId);
    }
}
