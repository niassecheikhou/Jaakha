package sn.jaakha.backend.application.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
