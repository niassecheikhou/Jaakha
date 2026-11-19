package sn.jaakha.backend.presentation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.ReviewService;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {
    @Mock
    private ProviderService providerService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private StorageService storageService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ProviderController providerController;

    private final MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

    @Test
    void anonymousUploadIsRejectedBeforeAnyFileIsWritten() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserService.requireUser(request)).thenThrow(new UnauthorizedException("non connecte"));

        assertThatThrownBy(() -> providerController.uploadPhoto(request, file))
                .isInstanceOf(UnauthorizedException.class);
        verify(storageService, never()).storeProviderPhoto(any());
    }

    @Test
    void nonProviderUploadIsRejectedBeforeAnyFileIsWritten() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthenticatedUser employer = new AuthenticatedUser(UUID.randomUUID(), "Moussa Fall", "EMPLOYEUR");
        when(currentUserService.requireUser(request)).thenReturn(employer);
        doThrow(new ForbiddenException("prestataire requis")).when(providerService).requireProvider(employer);

        assertThatThrownBy(() -> providerController.uploadPhoto(request, file))
                .isInstanceOf(ForbiddenException.class);
        verify(storageService, never()).storeProviderPhoto(any());
    }
}
