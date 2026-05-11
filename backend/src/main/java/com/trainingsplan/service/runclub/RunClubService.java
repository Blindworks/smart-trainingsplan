package com.trainingsplan.service.runclub;

import com.trainingsplan.dto.runclub.*;
import com.trainingsplan.entity.*;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.GroupEventRepository;
import com.trainingsplan.repository.RunClubMembershipRepository;
import com.trainingsplan.repository.RunClubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RunClubService {

    private static final Logger log = LoggerFactory.getLogger(RunClubService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int SLUG_MAX_LENGTH = 60;

    private final RunClubRepository runClubRepository;
    private final RunClubMembershipRepository membershipRepository;
    private final GroupEventRepository groupEventRepository;
    private final ImageStoragePort imageStoragePort;

    public RunClubService(RunClubRepository runClubRepository,
                          RunClubMembershipRepository membershipRepository,
                          GroupEventRepository groupEventRepository,
                          ImageStoragePort imageStoragePort) {
        this.runClubRepository = runClubRepository;
        this.membershipRepository = membershipRepository;
        this.groupEventRepository = groupEventRepository;
        this.imageStoragePort = imageStoragePort;
    }

    // ---- Creation ----

    public RunClubDto create(User creator, CreateRunClubRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Club name is required");
        }
        if (runClubRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A run club with this name already exists");
        }

        RunClub club = new RunClub();
        club.setName(request.getName());
        club.setSlug(generateUniqueSlug(request.getName(), null));
        club.setDescription(request.getDescription());
        club.setLocationCity(request.getLocationCity());
        club.setLocationCountry(request.getLocationCountry());
        club.setLatitude(request.getLatitude());
        club.setLongitude(request.getLongitude());
        club.setMeetingPointName(request.getMeetingPointName());
        club.setMeetingSchedule(request.getMeetingSchedule());
        club.setMeetingTime(request.getMeetingTime());
        if (request.getMeetingDays() != null) club.setMeetingDays(request.getMeetingDays());
        club.setJoinPolicy(request.getJoinPolicy() != null ? request.getJoinPolicy() : RunClubJoinPolicy.OPEN);
        club.setSocialInstagram(request.getSocialInstagram());
        club.setSocialWebsite(request.getSocialWebsite());
        club.setSocialStrava(request.getSocialStrava());
        club.setStatus(RunClubStatus.PENDING);
        club.setCreatedBy(creator);
        club.setCreatedAt(LocalDateTime.now());
        club.setVerified(false);

        club = runClubRepository.save(club);

        // Founder gets PENDING ADMIN membership — activated when club is approved
        RunClubMembership membership = new RunClubMembership();
        membership.setClub(club);
        membership.setUser(creator);
        membership.setRole(RunClubMemberRole.ADMIN);
        membership.setStatus(RunClubMembershipStatus.PENDING);
        membership.setRequestedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        log.info("Run club created: id={} name='{}' by userId={}", club.getId(), club.getName(), creator.getId());
        return toDto(club, null);
    }

    // ---- Admin approval ----

    public RunClubDto approve(User adminUser, Long clubId) {
        RunClub club = requireClub(clubId);
        if (club.getStatus() != RunClubStatus.PENDING) {
            throw new IllegalStateException("Club is not in PENDING status");
        }
        club.setStatus(RunClubStatus.APPROVED);
        club.setApprovedAt(LocalDateTime.now());
        club.setApprovedBy(adminUser);
        club = runClubRepository.save(club);

        // Activate founder's membership
        membershipRepository.findByClubAndStatus(club, RunClubMembershipStatus.PENDING)
                .forEach(m -> {
                    if (m.getRole() == RunClubMemberRole.ADMIN) {
                        m.setStatus(RunClubMembershipStatus.ACTIVE);
                        m.setJoinedAt(LocalDateTime.now());
                        membershipRepository.save(m);
                    }
                });

        log.info("Run club approved: id={} by adminId={}", clubId, adminUser.getId());
        return toDto(club, adminUser);
    }

    public RunClubDto reject(User adminUser, Long clubId, String reason) {
        RunClub club = requireClub(clubId);
        if (club.getStatus() != RunClubStatus.PENDING) {
            throw new IllegalStateException("Club is not in PENDING status");
        }
        club.setStatus(RunClubStatus.REJECTED);
        club.setRejectionReason(reason);
        club = runClubRepository.save(club);
        log.info("Run club rejected: id={} by adminId={}", clubId, adminUser.getId());
        return toDto(club, adminUser);
    }

    public RunClubDto verify(User adminUser, Long clubId) {
        RunClub club = requireClub(clubId);
        club.setVerified(true);
        return toDto(runClubRepository.save(club), adminUser);
    }

    // ---- Update / Delete ----

    public RunClubDto update(User requestingUser, Long clubId, UpdateRunClubRequest request) {
        RunClub club = requireClub(clubId);
        requireClubAdmin(club, requestingUser);

        if (request.getName() != null && !request.getName().equals(club.getName())) {
            if (runClubRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("A run club with this name already exists");
            }
            club.setName(request.getName());
            club.setSlug(generateUniqueSlug(request.getName(), clubId));
        }
        if (request.getDescription() != null) club.setDescription(request.getDescription());
        if (request.getLocationCity() != null) club.setLocationCity(request.getLocationCity());
        if (request.getLocationCountry() != null) club.setLocationCountry(request.getLocationCountry());
        if (request.getLatitude() != null) club.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) club.setLongitude(request.getLongitude());
        if (request.getMeetingPointName() != null) club.setMeetingPointName(request.getMeetingPointName());
        if (request.getMeetingSchedule() != null) club.setMeetingSchedule(request.getMeetingSchedule());
        if (request.getMeetingTime() != null) club.setMeetingTime(request.getMeetingTime());
        if (request.getMeetingDays() != null) club.setMeetingDays(request.getMeetingDays());
        if (request.getJoinPolicy() != null) club.setJoinPolicy(request.getJoinPolicy());
        if (request.getSocialInstagram() != null) club.setSocialInstagram(request.getSocialInstagram());
        if (request.getSocialWebsite() != null) club.setSocialWebsite(request.getSocialWebsite());
        if (request.getSocialStrava() != null) club.setSocialStrava(request.getSocialStrava());

        return toDto(runClubRepository.save(club), requestingUser);
    }

    public void delete(User requestingUser, Long clubId) {
        RunClub club = requireClub(clubId);
        if (requestingUser.getRole() != UserRole.ADMIN) {
            requireClubAdmin(club, requestingUser);
        }
        runClubRepository.delete(club);
        log.warn("Run club deleted: id={} by userId={}", clubId, requestingUser.getId());
    }

    // ---- Image upload ----

    public RunClubDto uploadLogo(User requestingUser, Long clubId, MultipartFile file) {
        RunClub club = requireClub(clubId);
        requireClubAdmin(club, requestingUser);

        if (club.getLogoFilename() != null) {
            try { imageStoragePort.delete(club.getLogoFilename()); } catch (Exception ignored) {}
        }
        String filename = imageStoragePort.store(file);
        club.setLogoFilename(filename);
        return toDto(runClubRepository.save(club), requestingUser);
    }

    public RunClubDto uploadCover(User requestingUser, Long clubId, MultipartFile file) {
        RunClub club = requireClub(clubId);
        requireClubAdmin(club, requestingUser);

        if (club.getCoverImageFilename() != null) {
            try { imageStoragePort.delete(club.getCoverImageFilename()); } catch (Exception ignored) {}
        }
        String filename = imageStoragePort.store(file);
        club.setCoverImageFilename(filename);
        return toDto(runClubRepository.save(club), requestingUser);
    }

    public Resource loadImage(String filename) {
        return imageStoragePort.load(filename);
    }

    @Transactional(readOnly = true)
    public ClubImageData loadLogoImage(Long clubId) {
        return loadClubImage(requireClub(clubId).getLogoFilename());
    }

    @Transactional(readOnly = true)
    public ClubImageData loadCoverImage(Long clubId) {
        return loadClubImage(requireClub(clubId).getCoverImageFilename());
    }

    private ClubImageData loadClubImage(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        Resource resource = imageStoragePort.load(filename);
        String contentType = "application/octet-stream";
        try {
            String detected = java.nio.file.Files.probeContentType(resource.getFile().toPath());
            if (detected != null && !detected.isBlank()) {
                contentType = detected;
            }
        } catch (Exception ignored) {
        }
        return new ClubImageData(resource, contentType);
    }

    public record ClubImageData(Resource resource, String contentType) {}

    // ---- Search ----

    @Transactional(readOnly = true)
    public List<RunClubDto> searchByCity(String city, User currentUser) {
        return runClubRepository.findApprovedByLocationCityIgnoreCase(city).stream()
                .map(c -> toDto(c, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunClubDto> searchNearby(double lat, double lng, double radiusKm, User currentUser) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        return runClubRepository.findApprovedInBoundingBox(
                        lat - latDelta, lat + latDelta,
                        lng - lngDelta, lng + lngDelta).stream()
                .filter(c -> haversineKm(lat, lng, c.getLatitude(), c.getLongitude()) <= radiusKm)
                .map(c -> toDto(c, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunClubDto> searchByMeetingDay(DayOfWeek day, User currentUser) {
        return runClubRepository.findApprovedByMeetingDay(day).stream()
                .map(c -> toDto(c, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunClubDto> searchByName(String q, User currentUser) {
        return runClubRepository.findApprovedByNameContaining(q).stream()
                .map(c -> toDto(c, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunClubDto> getAllApprovedForMap(User currentUser) {
        return runClubRepository.findAllApprovedForMap().stream()
                .map(c -> toDto(c, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public RunClubDetailDto getBySlug(String slug, User currentUser) {
        RunClub club = runClubRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Run club not found: " + slug));
        return toDetailDto(club, currentUser);
    }

    @Transactional(readOnly = true)
    public RunClubDetailDto getById(Long id, User currentUser) {
        return toDetailDto(requireClub(id), currentUser);
    }

    @Transactional(readOnly = true)
    public List<RunClubDto> getAllForAdmin(User adminUser) {
        return runClubRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(c -> toDto(c, adminUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunClubDto> getPendingForAdmin(User adminUser) {
        return runClubRepository.findByStatus(RunClubStatus.PENDING,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "createdAt"))).stream()
                .map(c -> toDto(c, adminUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RunClubMembershipDto> getMyClubs(User user) {
        return membershipRepository.findByUserAndStatus(user, RunClubMembershipStatus.ACTIVE).stream()
                .map(this::toMembershipDto)
                .toList();
    }

    // ---- DTO mapping ----

    public RunClubDto toDto(RunClub club, User currentUser) {
        RunClubDto dto = new RunClubDto();
        mapBasicFields(club, dto, currentUser);
        return dto;
    }

    public RunClubDetailDto toDetailDto(RunClub club, User currentUser) {
        RunClubDetailDto dto = new RunClubDetailDto();
        mapBasicFields(club, dto, currentUser);

        if (currentUser != null) {
            Optional<RunClubMembership> membership = membershipRepository.findByClubAndUser(club, currentUser);
            membership.ifPresent(m -> {
                dto.setMember(m.getStatus() == RunClubMembershipStatus.ACTIVE);
                dto.setMyRole(m.getRole());
                dto.setMyMembershipStatus(m.getStatus());
                dto.setMyMembershipId(m.getId());
            });
            boolean isMembershipAdmin = membership
                    .map(m -> m.getStatus() == RunClubMembershipStatus.ACTIVE
                            && m.getRole() == RunClubMemberRole.ADMIN)
                    .orElse(false);
            boolean isPlatformAdmin = currentUser.getRole() == UserRole.ADMIN;
            dto.setCanManage(isMembershipAdmin || isPlatformAdmin);
        }
        return dto;
    }

    private void mapBasicFields(RunClub club, RunClubDto dto, User currentUser) {
        dto.setId(club.getId());
        dto.setName(club.getName());
        dto.setSlug(club.getSlug());
        dto.setDescription(club.getDescription());
        dto.setLogoFilename(club.getLogoFilename());
        dto.setCoverImageFilename(club.getCoverImageFilename());
        dto.setHasLogo(club.getLogoFilename() != null && !club.getLogoFilename().isBlank());
        dto.setHasCover(club.getCoverImageFilename() != null && !club.getCoverImageFilename().isBlank());
        dto.setLocationCity(club.getLocationCity());
        dto.setLocationCountry(club.getLocationCountry());
        dto.setLatitude(club.getLatitude());
        dto.setLongitude(club.getLongitude());
        dto.setMeetingPointName(club.getMeetingPointName());
        dto.setMeetingSchedule(club.getMeetingSchedule());
        dto.setMeetingTime(club.getMeetingTime());
        dto.setMeetingDays(club.getMeetingDays());
        dto.setJoinPolicy(club.getJoinPolicy());
        dto.setStatus(club.getStatus());
        dto.setSocialInstagram(club.getSocialInstagram());
        dto.setSocialWebsite(club.getSocialWebsite());
        dto.setSocialStrava(club.getSocialStrava());
        dto.setVerified(club.isVerified());
        dto.setCreatedAt(club.getCreatedAt());
        if (club.getCreatedBy() != null) {
            dto.setCreatedById(club.getCreatedBy().getId());
            dto.setCreatedByUsername(club.getCreatedBy().getUsername());
        }
        dto.setMemberCount((int) membershipRepository.countByClubAndStatus(club, RunClubMembershipStatus.ACTIVE));
    }

    public RunClubMembershipDto toMembershipDto(RunClubMembership m) {
        RunClubMembershipDto dto = new RunClubMembershipDto();
        dto.setId(m.getId());
        dto.setClubId(m.getClub().getId());
        dto.setClubName(m.getClub().getName());
        dto.setClubSlug(m.getClub().getSlug());
        dto.setUserId(m.getUser().getId());
        dto.setUsername(m.getUser().getUsername());
        dto.setUserFirstName(m.getUser().getFirstName());
        dto.setUserLastName(m.getUser().getLastName());
        String profileImage = m.getUser().getProfileImageFilename();
        dto.setProfileImageFilename(profileImage);
        dto.setHasProfileImage(profileImage != null && !profileImage.isBlank());
        User creator = m.getClub().getCreatedBy();
        dto.setCreator(creator != null && creator.getId().equals(m.getUser().getId()));
        dto.setRole(m.getRole());
        dto.setStatus(m.getStatus());
        dto.setJoinedAt(m.getJoinedAt());
        dto.setRequestedAt(m.getRequestedAt());
        return dto;
    }

    // ---- Helpers ----

    public RunClub requireClub(Long clubId) {
        return runClubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Run club not found: " + clubId));
    }

    public void requireClubAdmin(Long clubId, User user) {
        requireClubAdmin(requireClub(clubId), user);
    }

    public void requireClubAdmin(RunClub club, User user) {
        if (user != null && user.getRole() == UserRole.ADMIN) {
            return; // Platform admins may manage any club
        }
        boolean isAdmin = membershipRepository.existsByClubAndUserAndStatus(club, user, RunClubMembershipStatus.ACTIVE)
                && membershipRepository.findByClubAndUser(club, user)
                .map(m -> m.getRole() == RunClubMemberRole.ADMIN)
                .orElse(false);
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Not a club admin");
        }
    }

    public boolean isClubAdmin(RunClub club, User user) {
        if (user != null && user.getRole() == UserRole.ADMIN) {
            return true; // Platform admins are treated as club admins everywhere
        }
        return membershipRepository.findByClubAndUser(club, user)
                .map(m -> m.getStatus() == RunClubMembershipStatus.ACTIVE && m.getRole() == RunClubMemberRole.ADMIN)
                .orElse(false);
    }

    public boolean isActiveMember(RunClub club, User user) {
        return membershipRepository.existsByClubAndUserAndStatus(club, user, RunClubMembershipStatus.ACTIVE);
    }

    public String generateUniqueSlug(String name, Long excludeId) {
        String base = slugify(name);
        if (base.length() > SLUG_MAX_LENGTH) base = base.substring(0, SLUG_MAX_LENGTH);

        String candidate = base;
        int suffix = 2;
        while (excludeId == null
                ? runClubRepository.existsBySlug(candidate)
                : runClubRepository.existsBySlugAndIdNot(candidate, excludeId)) {
            String sfx = "-" + suffix++;
            int maxBase = SLUG_MAX_LENGTH - sfx.length();
            candidate = base.substring(0, Math.min(base.length(), maxBase)) + sfx;
        }
        return candidate;
    }

    private static String slugify(String input) {
        // Replace umlauts BEFORE NFD decomposition so they are expanded correctly
        String expanded = input
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replace("Ä", "ae").replace("Ö", "oe").replace("Ü", "ue");
        // NFD decomposition + strip combining diacritics
        String normalized = Normalizer.normalize(expanded, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
