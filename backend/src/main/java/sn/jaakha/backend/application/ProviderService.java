package sn.jaakha.backend.application;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AvailabilityRequest;
import sn.jaakha.backend.application.dto.ProviderProfileRequest;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.domain.enums.NotificationType;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.ProviderAvailability;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.ProviderAvailabilityRepository;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;

@Service
public class ProviderService {
    private final ProviderProfileRepository providerRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final NotificationService notificationService;

    public ProviderService(ProviderProfileRepository providerRepository,
            ProviderAvailabilityRepository availabilityRepository, NotificationService notificationService) {
        this.providerRepository = providerRepository;
        this.availabilityRepository = availabilityRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProviderProfileResponse saveMyProfile(AppUser user, ProviderProfileRequest request) {
        if (user.getRole() != Role.PRESTATAIRE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seuls les prestataires peuvent creer un profil.");
        }
        ProviderProfile profile = providerRepository.findByUser(user).orElseGet(() -> new ProviderProfile(user));
        profile.setPhotoUrl(blankToNull(request.photoUrl()));
        profile.setBio(blankToNull(request.bio()));
        profile.setZone(request.zone().trim());
        profile.setHourlyRate(request.hourlyRate());
        profile.setExperienceYears(request.experienceYears());
        profile.setSkills(cleanSkills(request.skills()));
        ProviderProfile saved = providerRepository.save(profile);

        availabilityRepository.deleteByProvider(saved);
        if (request.availability() != null) {
            request.availability().forEach(slot -> availabilityRepository.save(toAvailability(saved, slot)));
        }
        notificationService.notifyAdmins(
                NotificationType.PROVIDER_PENDING_VALIDATION,
                "Profil prestataire a verifier",
                user.getFullName() + " a renseigne ou modifie son profil prestataire.",
                null,
                user.getId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProviderProfileResponse getById(UUID id) {
        ProviderProfile profile = providerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prestataire introuvable."));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProviderProfileResponse getMine(AppUser user) {
        ProviderProfile profile = providerRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil prestataire introuvable."));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> search(String zone, BigDecimal maxHourlyRate, BigDecimal minRating,
            String skill, DayOfWeek availableDay) {
        return providerRepository.findByStatusOrderByCreatedAtDesc(ProviderStatus.APPROVED)
                .stream()
                .filter(profile -> zone == null || profile.getZone().equalsIgnoreCase(zone.trim()))
                .filter(profile -> maxHourlyRate == null || profile.getHourlyRate().compareTo(maxHourlyRate) <= 0)
                .filter(profile -> minRating == null || profile.getAverageRating().compareTo(minRating) >= 0)
                .filter(profile -> skill == null || profile.getSkills().stream().anyMatch(saved -> saved.contains(skill.toLowerCase(Locale.ROOT).trim())))
                .filter(profile -> availableDay == null || availabilityRepository.findByProvider(profile).stream().anyMatch(slot -> slot.getDayOfWeek() == availableDay))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProviderProfileResponse updateStatus(UUID id, ProviderStatus status) {
        ProviderProfile profile = providerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prestataire introuvable."));
        profile.setStatus(status);
        return toResponse(providerRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> listAll(ProviderStatus status) {
        List<ProviderProfile> profiles = status == null
                ? providerRepository.findAllByOrderByCreatedAtDesc()
                : providerRepository.findByStatusOrderByCreatedAtDesc(status);
        return profiles.stream().map(this::toResponse).toList();
    }

    private ProviderAvailability toAvailability(ProviderProfile profile, AvailabilityRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fin de disponibilite doit etre apres le debut.");
        }
        return new ProviderAvailability(profile, request.dayOfWeek(), request.startTime(), request.endTime());
    }

    private ProviderProfileResponse toResponse(ProviderProfile profile) {
        return ProviderProfileResponse.from(profile, availabilityRepository.findByProvider(profile));
    }

    private LinkedHashSet<String> cleanSkills(java.util.Set<String> skills) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        if (skills != null) {
            skills.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .map(skill -> skill.trim().toLowerCase(Locale.ROOT))
                    .forEach(cleaned::add);
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
