package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotNull;

public record AccountStatusUpdateRequest(
        @NotNull(message = "Le statut du compte est obligatoire.") Boolean active
) {
}
