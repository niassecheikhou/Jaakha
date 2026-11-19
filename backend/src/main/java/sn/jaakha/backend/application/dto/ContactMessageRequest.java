package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 40) String phone,
        @Size(max = 160) String company,
        @NotBlank @Size(max = 2000) String message
) {
}
