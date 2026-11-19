package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.jaakha.backend.application.dto.AccountProfileRequest;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
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
}
