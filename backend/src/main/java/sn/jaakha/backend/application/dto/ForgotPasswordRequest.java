package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "L'email ou le telephone est obligatoire.") String identifier
) {
}
