package sn.jaakha.backend.application.dto;

/**
 * URLs protegees du recto et du verso de la piece d'identite (null si la face n'est pas deposee).
 */
public record IdentityPhotosResponse(
        String identityPhotoUrl,
        String identityPhotoBackUrl
) {
}
