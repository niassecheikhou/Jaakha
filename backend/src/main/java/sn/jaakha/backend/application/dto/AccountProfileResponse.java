package sn.jaakha.backend.application.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import sn.jaakha.backend.domain.model.AccountProfile;

public record AccountProfileResponse(
        UUID id,
        String userId,
        String photoUrl,
        String identityPhotoUrl,
        String bio,
        String zone,
        BigDecimal monthlyRate,
        int experienceYears,
        List<String> skills,
        String guardianFullName,
        String guardianPhone
) {
    public static final String OWNER_IDENTITY_PHOTO_URL = "/api/account/me/identity-photo";

    public static AccountProfileResponse from(AccountProfile profile) {
        return new AccountProfileResponse(
                profile.getId(),
                profile.getOwnerKey(),
                profile.getPhotoUrl(),
                profile.getIdentityPhotoUrl() == null ? null : OWNER_IDENTITY_PHOTO_URL,
                profile.getBio(),
                profile.getZone(),
                profile.getMonthlyRate(),
                profile.getExperienceYears(),
                splitSkills(profile.getSkills()),
                profile.getGuardianFullName(),
                profile.getGuardianPhone()
        );
    }

    private static List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
