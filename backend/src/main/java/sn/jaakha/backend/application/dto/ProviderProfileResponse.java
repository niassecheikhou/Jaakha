package sn.jaakha.backend.application.dto;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderProfile;

public record ProviderProfileResponse(
        UUID id,
        String userId,
        String fullName,
        String photoUrl,
        String bio,
        String zone,
        BigDecimal monthlyRate,
        int experienceYears,
        Set<String> skills,
        ProviderStatus status,
        BigDecimal averageRating,
        int reviewCount,
        List<AvailabilityResponse> availability
) {
    public static ProviderProfileResponse from(ProviderProfile profile) {
        return new ProviderProfileResponse(
                profile.getId(),
                profile.getUserId().toString(),
                profile.getFullName(),
                profile.getPhotoUrl(),
                profile.getBio(),
                profile.getZone(),
                profile.getHourlyRate(),
                profile.getExperienceYears(),
                new LinkedHashSet<>(profile.getSkills()),
                profile.getStatus(),
                profile.getAverageRating(),
                profile.getReviewCount(),
                List.of()
        );
    }
}
