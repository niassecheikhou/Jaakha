package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotNull;

public record AccountArchiveRequest(
        @NotNull(message = "Le statut d'archivage est obligatoire.") Boolean archived
) {
}
