package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingAdminRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    private BookingAdminRepository bookingAdminRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ProviderProfileRepository providerProfileRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private MailService mailService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void employerWithoutIdentityBackCannotBeValidated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(employer(id, "/front", null)));

        assertThatThrownBy(() -> adminService.updateUserActive(id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verso de la piece d'identite")
                .hasMessageNotContaining("recto");
        verify(userAccountRepository, never()).updateActive(any(), anyBoolean());
    }

    @Test
    void employerWithoutIdentityFrontCannotBeValidated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(employer(id, null, "/back")));

        assertThatThrownBy(() -> adminService.updateUserActive(id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recto de la piece d'identite");
    }

    @Test
    void employerWithBothSidesCanBeValidated() {
        UUID id = UUID.randomUUID();
        UserResponse complete = employer(id, "/front", "/back");
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(complete));

        adminService.updateUserActive(id, true);

        verify(userAccountRepository).updateActive(id, true);
    }

    private UserResponse employer(UUID id, String identityFront, String identityBack) {
        return new UserResponse(
                id, "Moussa Fall", "moussa@jaankha.test", "+221771234567", "EMPLOYEUR",
                false, false, false,
                "http://localhost/uploads/profiles/photo.jpg", identityFront, identityBack,
                null, "Dakar", null, 0, List.of(), null, null, "2026-11-01T10:00:00Z"
        );
    }
}
