package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OtpRequest(
        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email doit etre valide.")
        String email
) {
}
