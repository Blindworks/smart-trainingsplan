package com.trainingsplan.service;

import com.trainingsplan.entity.BuddyRun;
import com.trainingsplan.entity.BuddyRunParticipant;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotification;
import com.trainingsplan.repository.BuddyRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class BuddyReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(BuddyReminderScheduler.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final BuddyRunRepository buddyRunRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public BuddyReminderScheduler(BuddyRunRepository buddyRunRepository,
                                  NotificationService notificationService,
                                  EmailService emailService) {
        this.buddyRunRepository = buddyRunRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void runReminders() {
        LocalDateTime now = LocalDateTime.now();
        sendWindow(now, 24, UserNotification.Type.BUDDY_REMINDER_24H, "24 Stunden");
        sendWindow(now, 1, UserNotification.Type.BUDDY_REMINDER_1H, "1 Stunde");
    }

    private void sendWindow(LocalDateTime now, int hoursAhead, UserNotification.Type type, String label) {
        LocalDateTime from = now.plusHours(hoursAhead).minusMinutes(2);
        LocalDateTime to = now.plusHours(hoursAhead).plusMinutes(5);
        List<BuddyRun> runs = buddyRunRepository.findActiveBetween(from, to);
        for (BuddyRun br : runs) {
            for (BuddyRunParticipant p : br.getParticipants()) {
                if (p.getStatus() != BuddyRunParticipant.Status.JOINED) continue;
                User u = p.getUser();
                if (u == null) continue;
                if (notificationService.alreadyNotified(u, type, br.getId())) continue;
                String title = "Erinnerung: " + br.getTitle();
                String msg = "Dein Buddy-Run startet in " + label + " am " + br.getMeetingPointName() + ".";
                notificationService.notify(u, type, title, msg, "/buddy/" + br.getId(), br.getId());
                try {
                    if (u.getEmail() != null && !u.getEmail().isBlank()) {
                        emailService.sendBuddyReminder(u.getEmail(), br.getTitle(), br.getMeetingPointName(),
                                br.getScheduledAt().format(DT_FMT), label, br.getId());
                    }
                } catch (Exception e) {
                    log.warn("Failed to send buddy reminder email to {}: {}", u.getEmail(), e.getMessage());
                }
            }
        }
    }
}
