package com.trainingsplan.service.runclub;

import com.trainingsplan.dto.runclub.RunClubMembershipDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.RunClubMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RunClubMembershipService {

    private static final Logger log = LoggerFactory.getLogger(RunClubMembershipService.class);

    private final RunClubMembershipRepository membershipRepository;
    private final RunClubService runClubService;

    public RunClubMembershipService(RunClubMembershipRepository membershipRepository,
                                    RunClubService runClubService) {
        this.membershipRepository = membershipRepository;
        this.runClubService = runClubService;
    }

    public RunClubMembershipDto join(User user, Long clubId) {
        RunClub club = runClubService.requireClub(clubId);
        if (club.getStatus() != RunClubStatus.APPROVED) {
            throw new IllegalStateException("Club is not available for joining");
        }

        membershipRepository.findByClubAndUser(club, user).ifPresent(m -> {
            if (m.getStatus() == RunClubMembershipStatus.ACTIVE) {
                throw new IllegalStateException("Already a member of this club");
            }
            if (m.getStatus() == RunClubMembershipStatus.PENDING) {
                throw new IllegalStateException("Join request already pending");
            }
            // Previously LEFT — delete old record so we can re-join
            membershipRepository.delete(m);
        });

        RunClubMembership membership = new RunClubMembership();
        membership.setClub(club);
        membership.setUser(user);
        membership.setRole(RunClubMemberRole.MEMBER);
        membership.setRequestedAt(LocalDateTime.now());

        if (club.getJoinPolicy() == RunClubJoinPolicy.OPEN) {
            membership.setStatus(RunClubMembershipStatus.ACTIVE);
            membership.setJoinedAt(LocalDateTime.now());
            log.info("User {} joined club {} directly (OPEN)", user.getId(), clubId);
        } else {
            membership.setStatus(RunClubMembershipStatus.PENDING);
            log.info("User {} requested to join club {} (REQUEST)", user.getId(), clubId);
        }

        return runClubService.toMembershipDto(membershipRepository.save(membership));
    }

    public void leave(User user, Long clubId) {
        RunClub club = runClubService.requireClub(clubId);
        RunClubMembership membership = membershipRepository.findByClubAndUser(club, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this club"));

        if (membership.getStatus() != RunClubMembershipStatus.ACTIVE) {
            throw new IllegalStateException("Cannot leave: not an active member");
        }

        // Prevent last admin from leaving
        if (membership.getRole() == RunClubMemberRole.ADMIN) {
            long activeAdmins = membershipRepository.countByClubAndStatusAndRole(
                    club, RunClubMembershipStatus.ACTIVE, RunClubMemberRole.ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalStateException(
                        "Cannot leave: you are the last admin. Promote another member first.");
            }
        }

        membership.setStatus(RunClubMembershipStatus.LEFT);
        membershipRepository.save(membership);
        log.info("User {} left club {}", user.getId(), clubId);
    }

    public RunClubMembershipDto approveJoinRequest(User adminUser, Long membershipId) {
        RunClubMembership membership = requireMembership(membershipId);
        requireAdminOfClub(adminUser, membership.getClub());

        if (membership.getStatus() != RunClubMembershipStatus.PENDING) {
            throw new IllegalStateException("Membership is not in PENDING status");
        }
        membership.setStatus(RunClubMembershipStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        log.info("Join request {} approved by admin {}", membershipId, adminUser.getId());
        return runClubService.toMembershipDto(membershipRepository.save(membership));
    }

    public void rejectJoinRequest(User adminUser, Long membershipId) {
        RunClubMembership membership = requireMembership(membershipId);
        requireAdminOfClub(adminUser, membership.getClub());

        if (membership.getStatus() != RunClubMembershipStatus.PENDING) {
            throw new IllegalStateException("Membership is not in PENDING status");
        }
        membershipRepository.delete(membership);
        log.info("Join request {} rejected by admin {}", membershipId, adminUser.getId());
    }

    public RunClubMembershipDto promote(User adminUser, Long membershipId) {
        RunClubMembership membership = requireMembership(membershipId);
        requireAdminOfClub(adminUser, membership.getClub());

        if (membership.getStatus() != RunClubMembershipStatus.ACTIVE) {
            throw new IllegalStateException("Can only promote active members");
        }
        if (membership.getRole() == RunClubMemberRole.ADMIN) {
            throw new IllegalStateException("Member is already an admin");
        }
        membership.setRole(RunClubMemberRole.ADMIN);
        return runClubService.toMembershipDto(membershipRepository.save(membership));
    }

    public RunClubMembershipDto demote(User adminUser, Long membershipId) {
        RunClubMembership membership = requireMembership(membershipId);
        requireAdminOfClub(adminUser, membership.getClub());

        if (membership.getRole() != RunClubMemberRole.ADMIN) {
            throw new IllegalStateException("Member is not an admin");
        }
        long activeAdmins = membershipRepository.countByClubAndStatusAndRole(
                membership.getClub(), RunClubMembershipStatus.ACTIVE, RunClubMemberRole.ADMIN);
        if (activeAdmins <= 1) {
            throw new IllegalStateException("Cannot demote the last admin");
        }
        membership.setRole(RunClubMemberRole.MEMBER);
        return runClubService.toMembershipDto(membershipRepository.save(membership));
    }

    public void kick(User adminUser, Long membershipId) {
        RunClubMembership membership = requireMembership(membershipId);
        requireAdminOfClub(adminUser, membership.getClub());

        if (membership.getRole() == RunClubMemberRole.ADMIN) {
            long activeAdmins = membershipRepository.countByClubAndStatusAndRole(
                    membership.getClub(), RunClubMembershipStatus.ACTIVE, RunClubMemberRole.ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalStateException("Cannot kick the last admin");
            }
        }
        membership.setStatus(RunClubMembershipStatus.LEFT);
        membershipRepository.save(membership);
        log.info("Member {} kicked from club {} by admin {}", membership.getUser().getId(),
                membership.getClub().getId(), adminUser.getId());
    }

    @Transactional(readOnly = true)
    public List<RunClubMembershipDto> listMembers(Long clubId, RunClubMembershipStatus status) {
        return membershipRepository.findByClubIdAndStatus(clubId, status).stream()
                .map(runClubService::toMembershipDto)
                .toList();
    }

    private RunClubMembership requireMembership(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
    }

    private void requireAdminOfClub(User user, RunClub club) {
        if (!runClubService.isClubAdmin(club, user) && user.getRole() != UserRole.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Not a club admin");
        }
    }
}
