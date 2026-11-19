package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'email ou le telephone est obligatoire.") String identifier,
        @NotBlank(message = "Le mot de passe est obligatoire.") String password
) {
}
