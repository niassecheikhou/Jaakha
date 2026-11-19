package sn.jaakha.backend.application;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.ProviderProfileRequest;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.AccountProfile;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.AccountProfileRepository;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;
import sn.jaakha.backend.presentation.ForbiddenException;

@Service
public class ProviderService {
    private final ProviderProfileRepository providerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccountProfileRepository accountProfileRepository;

    public ProviderService(
            ProviderProfileRepository providerProfileRepository,
            UserAccountRepository userAccountRepository,
            AccountProfileRepository accountProfileRepository
    ) {
        this.providerProfileRepository = providerProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.accountProfileRepository = accountProfileRepository;
    }

    /**
     * Seul un compte PRESTATAIRE peut posseder et modifier un profil prestataire.
     */
    public void requireProvider(AuthenticatedUser user) {
        if (!"PRESTATAIRE".equals(user.role())) {
            throw new ForbiddenException("Seuls les comptes prestataires peuvent gerer un profil prestataire.");
        }
    }

    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> search(String zone, String skill, BigDecimal maxPrice) {
        Set<UUID> visibleProviderUsers = userAccountRepository.visibleProviderUserIds();
        return providerProfileRepository.findAll().stream()
                .filter(profile -> profile.getStatus() == ProviderStatus.APPROVED)
                .filter(profile -> visibleProviderUsers.contains(profile.getUserId()))
                .filter(profile -> matchesZone(profile, zone))
                .filter(profile -> matchesSkill(profile, skill))
                .filter(profile -> matchesPrice(profile, maxPrice))
                .map(ProviderProfileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderProfileResponse getById(UUID id) {
        return providerProfileRepository.findById(id)
                .filter(profile -> profile.getStatus() == ProviderStatus.APPROVED)
                .filter(profile -> userAccountRepository.visibleProviderUserIds().contains(profile.getUserId()))
                .map(ProviderProfileResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Ce prestataire n'est pas encore valide par l'administration."));
    }

    @Transactional
    public ProviderProfileResponse getMine(AuthenticatedUser user) {
        requireProvider(user);
        return ProviderProfileResponse.from(getOrCreateProfile(user));
    }

    @Transactional
    public ProviderProfileResponse saveMine(AuthenticatedUser user, ProviderProfileRequest request) {
        requireProvider(user);
        ProviderProfile profile = getOrCreateProfile(user);
        // La photo publique n'est jamais prise dans la requete : on reprend celle deposee via l'upload du compte.
        String accountPhoto = uploadedAccountPhoto(user);
        if (accountPhoto != null) {
            profile.setPhotoUrl(accountPhoto);
        }
        profile.setBio(blankToNull(request.bio()));
        profile.setZone(request.zone().trim());
        profile.setHourlyRate(request.monthlyRate());
        profile.setExperienceYears(request.experienceYears());
        profile.setSkills(cleanSkills(request.skills()));
        return ProviderProfileResponse.from(providerProfileRepository.save(profile));
    }

    @Transactional
    public ProviderProfileResponse updatePhoto(AuthenticatedUser user, String photoUrl) {
        requireProvider(user);
        ProviderProfile profile = getOrCreateProfile(user);
        profile.setPhotoUrl(photoUrl);
        return ProviderProfileResponse.from(providerProfileRepository.save(profile));
    }

    private ProviderProfile getOrCreateProfile(AuthenticatedUser user) {
        String ownerKey = user.id().toString();
        return providerProfileRepository.findByUserId(user.id())
                .or(() -> providerProfileRepository.findByOwnerKey(ownerKey))
                .orElseGet(() -> providerProfileRepository.save(new ProviderProfile(ownerKey, user.id(), user.fullName())));
    }

    private String uploadedAccountPhoto(AuthenticatedUser user) {
        return accountProfileRepository.findByOwnerKey(user.id().toString())
                .map(AccountProfile::getPhotoUrl)
                .orElse(null);
    }

    private Set<String> cleanSkills(List<String> skills) {
        if (skills == null) {
            return new LinkedHashSet<>();
        }
        Set<String> cleanValues = new LinkedHashSet<>();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                cleanValues.add(skill.trim());
            }
        }
        return cleanValues;
    }

    private boolean matchesZone(ProviderProfile profile, String zone) {
        return zone == null
                || zone.isBlank()
                || profile.getZone().toLowerCase(Locale.ROOT).contains(zone.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesSkill(ProviderProfile profile, String skill) {
        return skill == null
                || skill.isBlank()
                || profile.getSkills().stream()
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(skill.trim().toLowerCase(Locale.ROOT)));
    }

    private boolean matchesPrice(ProviderProfile profile, BigDecimal maxPrice) {
        return maxPrice == null || profile.getHourlyRate().compareTo(maxPrice) <= 0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
