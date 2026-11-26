package sn.jaakha.backend.application.dto;

import java.util.UUID;

/** Une entree de l'historique : qui (auteur) a fait quoi (action), sur quoi (cible), et quand. */
public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String actorRole,
        String action,
        String targetType,
        String targetId,
        String targetLabel,
        String details,
        // Date de l'action (ISO-8601).
        String createdAt
) {
}
