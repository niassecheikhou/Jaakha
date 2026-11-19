package sn.jaakha.backend.application;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import sn.jaakha.backend.presentation.ConflictException;
import sn.jaakha.backend.presentation.ForbiddenException;

/**
 * Cycle de vie d'une reservation : REQUESTED -> ACCEPTED -> IN_PROGRESS -> DONE,
 * avec annulation possible tant que la mission n'est pas terminee. DONE et CANCELLED sont definitifs.
 *
 * Droits par role :
 * - EMPLOYEUR : peut seulement annuler, et uniquement avant le debut de la mission ;
 * - PRESTATAIRE : peut accepter, demarrer puis terminer, dans cet ordre ;
 * - ADMIN : peut effectuer toute transition valide du cycle.
 */
public final class BookingStatusPolicy {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "REQUESTED", Set.of("ACCEPTED", "CANCELLED"),
            "ACCEPTED", Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("DONE", "CANCELLED"),
            "DONE", Set.of(),
            "CANCELLED", Set.of()
    );
    private static final Set<String> EMPLOYER_CANCELLABLE = Set.of("REQUESTED", "ACCEPTED");
    private static final Set<String> PROVIDER_TARGETS = Set.of("ACCEPTED", "IN_PROGRESS", "DONE");

    private BookingStatusPolicy() {
    }

    /**
     * Normalise et valide un statut recu de l'API.
     */
    public static String normalize(String status) {
        String cleanStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!TRANSITIONS.containsKey(cleanStatus)) {
            throw new IllegalArgumentException("Le statut de reservation est invalide.");
        }
        return cleanStatus;
    }

    /**
     * Verifie que le role peut faire passer la reservation de currentStatus a targetStatus.
     */
    public static void assertTransition(String role, String currentStatus, String targetStatus) {
        switch (role) {
            case "EMPLOYEUR" -> {
                if (!"CANCELLED".equals(targetStatus)) {
                    throw new ForbiddenException("Un employeur peut seulement annuler sa reservation.");
                }
                if (!EMPLOYER_CANCELLABLE.contains(currentStatus)) {
                    throw new ConflictException("Cette reservation ne peut plus etre annulee : la mission a deja commence ou est cloturee.");
                }
            }
            case "PRESTATAIRE" -> {
                if (!PROVIDER_TARGETS.contains(targetStatus)) {
                    throw new ForbiddenException("Un prestataire peut accepter, demarrer ou terminer une mission.");
                }
            }
            case "ADMIN" -> {
                // L'administration suit le meme cycle, sans restriction de role.
            }
            default -> throw new ForbiddenException("Ton role ne permet pas de modifier une reservation.");
        }
        if (!TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new ConflictException(
                    "Transition impossible : une reservation " + currentStatus + " ne peut pas passer a " + targetStatus + "."
            );
        }
    }
}
