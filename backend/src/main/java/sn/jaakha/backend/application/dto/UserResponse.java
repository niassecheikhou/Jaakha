package sn.jaakha.backend.application.dto;

import java.util.UUID;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.model.AppUser;

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
    }
}
