package sn.jaakha.backend.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AccountProfileRequest;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.domain.model.AccountProfile;
import sn.jaakha.backend.domain.repository.AccountProfileRepository;

@Service
public class AccountProfileService {
    private final AccountProfileRepository accountProfileRepository;

    public AccountProfileService(AccountProfileRepository accountProfileRepository) {
        this.accountProfileRepository = accountProfileRepository;
    }

    @Transactional
    public AccountProfileResponse getMine(String ownerKey) {
        return AccountProfileResponse.from(getOrCreate(ownerKey));
    }

    @Transactional
    public AccountProfileResponse updatePhoto(String ownerKey, String photoUrl) {
        AccountProfile profile = getOrCreate(ownerKey);
        profile.setPhotoUrl(photoUrl);
        return AccountProfileResponse.from(accountProfileRepository.save(profile));
    }

    @Transactional
    public AccountProfileResponse updateIdentityPhoto(String ownerKey, String identityPhotoUrl) {
        AccountProfile profile = getOrCreate(ownerKey);
        profile.setIdentityPhotoUrl(identityPhotoUrl);
        return AccountProfileResponse.from(accountProfileRepository.save(profile));
    }

    @Transactional
    public AccountProfileResponse saveMine(String ownerKey, AccountProfileRequest request) {
        AccountProfile profile = getOrCreate(ownerKey);
        profile.setPhotoUrl(blankToNull(request.photoUrl()));
        profile.setIdentityPhotoUrl(blankToNull(request.identityPhotoUrl()));
        profile.setBio(blankToNull(request.bio()));
        profile.setZone(blankToNull(request.zone()));
        profile.setMonthlyRate(request.monthlyRate());
        profile.setExperienceYears(request.experienceYears());
        profile.setSkills(joinSkills(request.skills()));
        profile.setGuardianFullName(blankToNull(request.guardianFullName()));
        profile.setGuardianPhone(blankToNull(request.guardianPhone()));
        return AccountProfileResponse.from(accountProfileRepository.save(profile));
    }

    private AccountProfile getOrCreate(String ownerKey) {
        return accountProfileRepository.findByOwnerKey(ownerKey)
                .orElseGet(() -> accountProfileRepository.save(new AccountProfile(ownerKey)));
    }

    private String joinSkills(Iterable<String> skills) {
        if (skills == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(skill.trim());
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
