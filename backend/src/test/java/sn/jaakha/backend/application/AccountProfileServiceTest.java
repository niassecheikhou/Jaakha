package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.jaakha.backend.application.dto.AccountProfileRequest;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.domain.enums.IdentityDocumentSide;
import sn.jaakha.backend.domain.model.AccountProfile;
import sn.jaakha.backend.domain.repository.AccountProfileRepository;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@ExtendWith(MockitoExtension.class)
class AccountProfileServiceTest {
    private static final String OWNER = "4b6f6f0e-0000-0000-0000-000000000001";
    private static final String PHOTO = "http://localhost:8080/uploads/profiles/photo.jpg";

    @Mock
    private AccountProfileRepository accountProfileRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private AccountProfileService accountProfileService;

    /**
     * Regression : enregistrer le dossier (sans photoUrl dans la requete) ne doit jamais effacer
     * la photo deposee par upload. Sinon elle disparait a l'actualisation de la page.
     */
    @Test
    void savingTheDossierKeepsTheUploadedPhoto() {
        AccountProfile profile = new AccountProfile(OWNER);
        profile.setPhotoUrl(PHOTO);
        when(accountProfileRepository.findByOwnerKey(OWNER)).thenReturn(Optional.of(profile));
        when(accountProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountProfileResponse response = accountProfileService.saveMine(OWNER, new AccountProfileRequest(
                "Bio", "Dakar", new BigDecimal("50000"), 2, List.of("menage"), "Tuteur Test", "+221771234567"
        ));

        assertThat(response.photoUrl()).isEqualTo(PHOTO);
        assertThat(profile.getPhotoUrl()).isEqualTo(PHOTO);
    }

    @Test
    void uploadingTheBackKeepsTheFrontAndDeletesTheReplacedBack() {
        AccountProfile profile = new AccountProfile(OWNER);
        profile.setIdentityPhoto(IdentityDocumentSide.FRONT, "recto.jpg");
        profile.setIdentityPhoto(IdentityDocumentSide.BACK, "ancien-verso.jpg");
        when(accountProfileRepository.findByOwnerKey(OWNER)).thenReturn(Optional.of(profile));
        when(accountProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountProfileResponse response = accountProfileService.updateIdentityPhotos(
                OWNER, Map.of(IdentityDocumentSide.BACK, "nouveau-verso.png")
        );

        assertThat(profile.getIdentityPhoto(IdentityDocumentSide.FRONT)).isEqualTo("recto.jpg");
        assertThat(profile.getIdentityPhoto(IdentityDocumentSide.BACK)).isEqualTo("nouveau-verso.png");
        assertThat(response.identityPhotoUrl()).isEqualTo("/api/account/me/identity-photo");
        assertThat(response.identityPhotoBackUrl()).isEqualTo("/api/account/me/identity-photo/back");
        verify(storageService).deleteIdentityPhoto("ancien-verso.jpg");
        verify(storageService, never()).deleteIdentityPhoto("recto.jpg");
    }

    @Test
    void missingBackSideIsReportedAsNotFound() {
        AccountProfile profile = new AccountProfile(OWNER);
        profile.setIdentityPhoto(IdentityDocumentSide.FRONT, "recto.jpg");
        when(accountProfileRepository.findByOwnerKey(OWNER)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> accountProfileService.identityPhoto(OWNER, IdentityDocumentSide.BACK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verso");
        verify(storageService, never()).loadIdentityPhoto(any());
    }
}
