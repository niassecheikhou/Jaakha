package sn.jaakha.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String role,
        boolean emailVerified,
        boolean active,
        boolean archived,
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
}
