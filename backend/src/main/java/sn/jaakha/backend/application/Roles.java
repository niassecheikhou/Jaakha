package sn.jaakha.backend.application;

/**
 * Roles de la plateforme.
 * - SUPER_ADMIN : tous les droits d'un administrateur, et lui seul cree et gere les comptes ADMIN.
 * - ADMIN : gere les comptes EMPLOYEUR et PRESTATAIRE, les profils et les reservations.
 * - EMPLOYEUR / PRESTATAIRE : utilisateurs de la plateforme.
 */
public final class Roles {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String EMPLOYEUR = "EMPLOYEUR";
    public static final String PRESTATAIRE = "PRESTATAIRE";

    private Roles() {
    }

    /** Vrai pour un compte d'administration (ADMIN ou SUPER_ADMIN). */
    public static boolean isAdministration(String role) {
        return ADMIN.equals(role) || SUPER_ADMIN.equals(role);
    }

    public static boolean isSuperAdmin(String role) {
        return SUPER_ADMIN.equals(role);
    }
}
