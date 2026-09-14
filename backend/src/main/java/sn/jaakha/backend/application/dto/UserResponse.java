package sn.jaakha.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean emailVerified,
        boolean active
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isEmailVerified(),
                user.isActive()
        );
    
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
