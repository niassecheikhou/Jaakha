package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email doit etre valide.")
        String email,

        @NotBlank(message = "Le code OTP est obligatoire.")
        @Size(min = 6, max = 6, message = "Le code OTP doit contenir exactement 6 chiffres.")
        @Pattern(regexp = "^[0-9]{6}$", message = "Le code OTP doit contenir uniquement 6 chiffres.")
        String code
) {
}
