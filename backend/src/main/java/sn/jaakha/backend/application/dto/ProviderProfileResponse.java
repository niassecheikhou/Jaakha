package sn.jaakha.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderAvailability;
import sn.jaakha.backend.domain.model.ProviderProfile;

public record ProviderProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String photoUrl,
        String bio,
        String zone,
        BigDecimal hourlyRate,
        int experienceYears,
        Set<String> skills,
        ProviderStatus status,
        BigDecimal averageRating,
        int reviewCount,
        List<AvailabilityResponse> availability
) {
    public static ProviderProfileResponse from(ProviderProfile profile, List<ProviderAvailability> availability) {
        return new ProviderProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getPhotoUrl(),
                profile.getBio(),
                profile.getZone(),
                profile.getHourlyRate(),
                profile.getExperienceYears(),
                profile.getSkills(),
                profile.getStatus(),
                profile.getAverageRating(),
                profile.getReviewCount(),
                availability.stream().map(AvailabilityResponse::from).toList()
        );
    }
}
