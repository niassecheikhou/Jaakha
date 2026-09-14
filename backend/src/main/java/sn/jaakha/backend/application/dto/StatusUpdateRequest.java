package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
        @NotBlank(message = "Le statut est obligatoire.") String status
) {
}
