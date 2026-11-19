package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.ProviderProfileRequest;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.domain.model.AccountProfile;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.AccountProfileRepository;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;
import sn.jaakha.backend.presentation.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {
    @Mock
    private ProviderProfileRepository providerProfileRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AccountProfileRepository accountProfileRepository;

    @InjectMocks
    private ProviderService providerService;

    private final AuthenticatedUser provider = new AuthenticatedUser(UUID.randomUUID(), "Awa Diop", "PRESTATAIRE");

    private final ProviderProfileRequest request = new ProviderProfileRequest(
            "Menage et repassage", "Dakar", new BigDecimal("75000"), 3, List.of("Menage"), List.of()
    );

    @Test
    void employerCannotManageProviderProfile() {
        AuthenticatedUser employer = new AuthenticatedUser(UUID.randomUUID(), "Moussa Fall", "EMPLOYEUR");

        assertThatThrownBy(() -> providerService.saveMine(employer, request)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> providerService.getMine(employer)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> providerService.requireProvider(employer)).isInstanceOf(ForbiddenException.class);
        verify(providerProfileRepository, never()).save(any());
    }

    @Test
    void savedPhotoComesFromUploadedAccountPhoto() {
        ProviderProfile profile = new ProviderProfile(provider.id().toString(), provider.id(), provider.fullName());
        AccountProfile account = new AccountProfile(provider.id().toString());
        account.setPhotoUrl("http://localhost:8080/uploads/profiles/photo.jpg");
        when(providerProfileRepository.findByUserId(provider.id())).thenReturn(Optional.of(profile));
        when(accountProfileRepository.findByOwnerKey(provider.id().toString())).thenReturn(Optional.of(account));
        when(providerProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderProfileResponse response = providerService.saveMine(provider, request);

        assertThat(response.photoUrl()).isEqualTo("http://localhost:8080/uploads/profiles/photo.jpg");
    }

    @Test
    void existingPhotoIsKeptWhenAccountHasNoUpload() {
        ProviderProfile profile = new ProviderProfile(provider.id().toString(), provider.id(), provider.fullName());
        profile.setPhotoUrl("http://localhost:8080/uploads/providers/old.jpg");
        when(providerProfileRepository.findByUserId(provider.id())).thenReturn(Optional.of(profile));
        when(accountProfileRepository.findByOwnerKey(provider.id().toString())).thenReturn(Optional.empty());
        when(providerProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderProfileResponse response = providerService.saveMine(provider, request);

        assertThat(response.photoUrl()).isEqualTo("http://localhost:8080/uploads/providers/old.jpg");
    }

    @Test
    void accountPhotoIsCopiedToPublicProfileForProviders() {
        ProviderProfile profile = new ProviderProfile(provider.id().toString(), provider.id(), provider.fullName());
        when(providerProfileRepository.findByUserId(provider.id())).thenReturn(Optional.of(profile));

        providerService.syncAccountPhoto(provider, "http://localhost:8080/uploads/profiles/new.jpg");

        assertThat(profile.getPhotoUrl()).isEqualTo("http://localhost:8080/uploads/profiles/new.jpg");
        verify(providerProfileRepository).save(profile);
    }

    @Test
    void accountPhotoIsNotCopiedForEmployersOrAdmins() {
        for (String role : List.of("EMPLOYEUR", "ADMIN")) {
            AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "Utilisateur", role);
            providerService.syncAccountPhoto(user, "http://localhost:8080/uploads/profiles/x.jpg");
        }

        verify(providerProfileRepository, never()).save(any());
        verify(providerProfileRepository, never()).findByUserId(any());
    }
}
