package sn.jaakha.backend.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
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
import sn.jaakha.backend.application.AuditLogService;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.IdentityPhotosResponse;
import sn.jaakha.backend.domain.enums.IdentityDocumentSide;
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
    private AuditLogService auditLogService;
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

    // --- Piece d'identite recto / verso ---

    private final MockMultipartFile front = new MockMultipartFile("front", "recto.jpg", "image/jpeg", new byte[]{1});
    private final MockMultipartFile back = new MockMultipartFile("back", "verso.png", "image/png", new byte[]{2});

    @Test
    void frontAndBackCanBeUploadedTogether() {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "Awa Diop", "PRESTATAIRE");
        when(currentUserService.requireUser(request)).thenReturn(user);
        when(storageService.storeIdentityPhoto(front)).thenReturn("recto.jpg");
        when(storageService.storeIdentityPhoto(back)).thenReturn("verso.png");
        AccountProfileResponse profile = mock(AccountProfileResponse.class);
        when(profile.identityPhotoUrl()).thenReturn(AccountProfileResponse.OWNER_IDENTITY_PHOTO_URL);
        when(profile.identityPhotoBackUrl()).thenReturn(AccountProfileResponse.OWNER_IDENTITY_PHOTO_BACK_URL);
        when(accountProfileService.updateIdentityPhotos(eq(user.id().toString()), any())).thenReturn(profile);

        IdentityPhotosResponse response = controller.uploadIdentityPhotos(request, front, back);

        assertThat(response.identityPhotoUrl()).isEqualTo("/api/account/me/identity-photo");
        assertThat(response.identityPhotoBackUrl()).isEqualTo("/api/account/me/identity-photo/back");
        verify(accountProfileService).updateIdentityPhotos(user.id().toString(), Map.of(
                IdentityDocumentSide.FRONT, "recto.jpg",
                IdentityDocumentSide.BACK, "verso.png"
        ));
    }

    @Test
    void oneSideCanBeUploadedAlone() {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "Moussa Fall", "EMPLOYEUR");
        when(currentUserService.requireUser(request)).thenReturn(user);
        when(storageService.storeIdentityPhoto(back)).thenReturn("verso.png");
        when(accountProfileService.updateIdentityPhotos(eq(user.id().toString()), any()))
                .thenReturn(mock(AccountProfileResponse.class));

        controller.uploadIdentityPhotos(request, null, back);

        verify(accountProfileService).updateIdentityPhotos(user.id().toString(), Map.of(IdentityDocumentSide.BACK, "verso.png"));
        verify(storageService, never()).storeIdentityPhoto(front);
    }

    @Test
    void uploadWithoutAnySideIsRejected() {
        when(currentUserService.requireUser(request))
                .thenReturn(new AuthenticatedUser(UUID.randomUUID(), "Moussa Fall", "EMPLOYEUR"));

        assertThatThrownBy(() -> controller.uploadIdentityPhotos(request, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(storageService, never()).storeIdentityPhoto(any());
    }

    @Test
    void invalidSecondFileMeansNothingIsWritten() {
        when(currentUserService.requireUser(request))
                .thenReturn(new AuthenticatedUser(UUID.randomUUID(), "Moussa Fall", "EMPLOYEUR"));
        // lenient : validateImage(front) est aussi appele (et accepte) avant le verso refuse.
        lenient().doThrow(new IllegalArgumentException("format refuse")).when(storageService).validateImage(back);

        assertThatThrownBy(() -> controller.uploadIdentityPhotos(request, front, back))
                .isInstanceOf(IllegalArgumentException.class);
        verify(storageService, never()).storeIdentityPhoto(any());
        verify(accountProfileService, never()).updateIdentityPhotos(anyString(), any());
    }

    @Test
    void anonymousIdentityUploadIsRejectedBeforeAnyFileIsWritten() {
        when(currentUserService.requireUser(request)).thenThrow(new UnauthorizedException("non connecte"));

        assertThatThrownBy(() -> controller.uploadIdentityPhotos(request, front, back))
                .isInstanceOf(UnauthorizedException.class);
        verify(storageService, never()).storeIdentityPhoto(any());
    }
}
