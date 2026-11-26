package sn.jaakha.backend.application;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AuditLogResponse;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.infrastructure.persistence.AuditLogRepository;

/**
 * Historique des actions des utilisateurs (qui a fait quoi, sur quoi, quand).
 * Les actions sont enregistrees apres leur reussite ; une erreur d'ecriture du journal est
 * journalisee mais ne fait jamais echouer l'action de l'utilisateur.
 */
@Service
public class AuditLogService {
    // Codes d'action (libelles en francais cote web).
    public static final String LOGIN = "CONNEXION";
    public static final String REGISTER = "INSCRIPTION";
    public static final String ACCOUNT_CREATED = "COMPTE_CREE";
    public static final String ACCOUNT_VALIDATED = "COMPTE_VALIDE";
    public static final String ACCOUNT_INVALIDATED = "COMPTE_INVALIDE";
    public static final String ACCOUNT_ARCHIVED = "COMPTE_ARCHIVE";
    public static final String ACCOUNT_RESTORED = "COMPTE_RESTAURE";
    public static final String PROFILE_UPDATED = "PROFIL_MIS_A_JOUR";
    public static final String PROFILE_PHOTO_UPDATED = "PHOTO_PROFIL_MODIFIEE";
    public static final String IDENTITY_UPLOADED = "PIECE_IDENTITE_DEPOSEE";
    public static final String PROVIDER_PROFILE_UPDATED = "PROFIL_PRESTATAIRE_MIS_A_JOUR";
    public static final String PROVIDER_STATUS_CHANGED = "PROFIL_PRESTATAIRE_STATUT";
    public static final String BOOKING_CREATED = "RESERVATION_CREEE";
    public static final String BOOKING_STATUS_CHANGED = "RESERVATION_STATUT";
    public static final String REVIEW_CREATED = "AVIS_PUBLIE";

    private static final Set<String> ACTIONS = Set.of(
            LOGIN, REGISTER, ACCOUNT_CREATED, ACCOUNT_VALIDATED, ACCOUNT_INVALIDATED, ACCOUNT_ARCHIVED,
            ACCOUNT_RESTORED, PROFILE_UPDATED, PROFILE_PHOTO_UPDATED, IDENTITY_UPLOADED,
            PROVIDER_PROFILE_UPDATED, PROVIDER_STATUS_CHANGED, BOOKING_CREATED, BOOKING_STATUS_CHANGED, REVIEW_CREATED
    );
    private static final Set<String> ROLES = Set.of(Roles.SUPER_ADMIN, Roles.ADMIN, Roles.EMPLOYEUR, Roles.PRESTATAIRE);
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(AuthenticatedUser actor, String action, String targetType, Object targetId, String targetLabel, String details) {
        record(actor.id(), actor.fullName(), actor.role(), action, targetType, targetId, targetLabel, details);
    }

    public void record(UserResponse actor, String action, String targetType, Object targetId, String targetLabel, String details) {
        record(actor.id(), actor.fullName(), actor.role(), action, targetType, targetId, targetLabel, details);
    }

    private void record(
            UUID actorId,
            String actorName,
            String actorRole,
            String action,
            String targetType,
            Object targetId,
            String targetLabel,
            String details
    ) {
        try {
            auditLogRepository.insert(
                    actorId,
                    truncate(actorName, 160),
                    actorRole,
                    action,
                    targetType,
                    targetId == null ? null : targetId.toString(),
                    truncate(targetLabel, 255),
                    truncate(details, 1000)
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Action {} de {} non enregistree dans l'historique", action, actorId, exception);
        }
    }

    /**
     * Recherche dans l'historique. Les filtres sont optionnels ; une valeur inconnue (date, role,
     * action) est refusee pour ne pas renvoyer silencieusement un resultat trompeur.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(String from, String to, UUID actorId, String actorRole, String action) {
        return auditLogRepository.search(
                parseInstant(from, "from"),
                parseInstant(to, "to"),
                actorId,
                allowed(actorRole, ROLES, "Le role"),
                allowed(action, ACTIONS, "L'action")
        );
    }

    private Instant parseInstant(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Le parametre " + name + " doit etre une date ISO-8601 (ex. 2026-11-26T00:00:00Z).");
        }
    }

    private String allowed(String value, Set<String> allowedValues, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!allowedValues.contains(value)) {
            throw new IllegalArgumentException(label + " " + value + " est inconnu.");
        }
        return value;
    }

    /** Libelle francais d'un statut de reservation, pour le detail de l'historique. */
    public static String bookingStatusLabel(String status) {
        if (status == null) {
            return "inconnu";
        }
        return switch (status) {
            case "REQUESTED" -> "en attente";
            case "ACCEPTED" -> "acceptee";
            case "IN_PROGRESS" -> "en cours";
            case "DONE" -> "terminee";
            case "CANCELLED" -> "annulee";
            default -> status;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
