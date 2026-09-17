package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Le jeton de reinitialisation est obligatoire.") String token,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$",
                message = "Le mot de passe doit contenir au moins 6 caracteres, une majuscule, une minuscule, un chiffre et un caractere special."
        ) String password
) {
}
