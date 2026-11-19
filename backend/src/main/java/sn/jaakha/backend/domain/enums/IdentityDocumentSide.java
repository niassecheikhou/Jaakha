package sn.jaakha.backend.domain.enums;

/**
 * Face de la piece d'identite (CNI) deposee par un employeur ou un prestataire.
 */
public enum IdentityDocumentSide {
    FRONT("recto"),
    BACK("verso");

    private final String label;

    IdentityDocumentSide(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
