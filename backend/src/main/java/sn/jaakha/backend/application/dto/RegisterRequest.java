package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.jaakha.backend.domain.enums.Role;

public record RegisterRequest(
        @NotBlank String fullName,
        String email,
        String phone,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role
) {
}
