package sn.jaakha.backend.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sn.jaakha.backend.application.AccountProfileService;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.PhotoUploadResponse;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@ExtendWith(MockitoExtension.class)
class AccountProfileControllerTest {
    private static final String STORED_URL = "/uploads/profiles/photo.jpg";
    private static final String ABSOLUTE_URL = "http://localhost" + STORED_URL;

    @Mock
    private AccountProfileService accountProfileService;
    @Mock
    private StorageService storageService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ProviderService providerService;

    @InjectMocks
    private AccountProfileController controller;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

    @BeforeEach
    void bindRequest() {
        // absoluteUrl() s'appuie sur la requete HTTP courante.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPLOYEUR", "PRESTATAIRE", "ADMIN"})
    void everyRoleCanUploadAProfilePhoto(String role) {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "Utilisateur " + role, role);
        AccountProfileResponse profile = mock(AccountProfileResponse.class);
        when(profile.photoUrl()).thenReturn(ABSOLUTE_URL);
        when(currentUserService.requireUser(request)).thenReturn(user);
        when(storageService.storeAccountPhoto(file)).thenReturn(STORED_URL);
        when(accountProfileService.updatePhoto(user.id().toString(), ABSOLUTE_URL)).thenReturn(profile);

        PhotoUploadResponse response = controller.uploadPhoto(request, file);

        assertThat(response.photoUrl()).isEqualTo(ABSOLUTE_URL);
        // La synchronisation du profil public est deleguee au service, qui ignore les non-prestataires.
        verify(providerService).syncAccountPhoto(user, ABSOLUTE_URL);
    }

    @Test
    void anonymousUploadIsRejectedBeforeAnyFileIsWritten() {
        when(currentUserService.requireUser(request)).thenThrow(new UnauthorizedException("non connecte"));

        assertThatThrownBy(() -> controller.uploadPhoto(request, file)).isInstanceOf(UnauthorizedException.class);
        verify(storageService, never()).storeAccountPhoto(any());
        verify(accountProfileService, never()).updatePhoto(anyString(), eq(ABSOLUTE_URL));
    }
}
