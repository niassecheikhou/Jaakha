package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingAdminRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;
import sn.jaakha.backend.presentation.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(UUID.randomUUID(), "Admin", "ADMIN");
    private static final AuthenticatedUser SUPER_ADMIN = new AuthenticatedUser(UUID.randomUUID(), "Super Admin", "SUPER_ADMIN");

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
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void employerWithoutIdentityBackCannotBeValidated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(employer(id, "/front", null)));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(ADMIN.id()));

        assertThatThrownBy(() -> adminService.updateUserActive(ADMIN, id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verso de la piece d'identite")
                .hasMessageNotContaining("recto");
        verify(userAccountRepository, never()).updateActive(any(), anyBoolean());
    }

    @Test
    void employerWithoutIdentityFrontCannotBeValidated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(employer(id, null, "/back")));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(ADMIN.id()));

        assertThatThrownBy(() -> adminService.updateUserActive(ADMIN, id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recto de la piece d'identite");
    }

    @Test
    void employerWithBothSidesCanBeValidated() {
        UUID id = UUID.randomUUID();
        UserResponse complete = employer(id, "/front", "/back");
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(complete));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(ADMIN.id()));

        adminService.updateUserActive(ADMIN, id, true);

        verify(userAccountRepository).updateActive(id, true);
    }

    @Test
    void adminCannotCreateAnAdminAccount() {
        assertThatThrownBy(() -> adminService.createUser(ADMIN, createRequest("ADMIN")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("super administrateur");
        verify(userAccountRepository, never()).create(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void superAdminCreatesAnAdminAccountPendingValidation() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userAccountRepository.findById(any())).thenAnswer(invocation ->
                Optional.of(account(invocation.getArgument(0), "ADMIN", null, null)));

        adminService.createUser(SUPER_ADMIN, createRequest("ADMIN"));

        verify(userAccountRepository).create(any(), any(), any(), any(), eq("hash"), eq("ADMIN"), eq(false), eq(SUPER_ADMIN.id()));
    }

    @Test
    void adminCreatesInactiveEmployerAccountLinkedToHim() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userAccountRepository.findById(any())).thenAnswer(invocation ->
                Optional.of(account(invocation.getArgument(0), "EMPLOYEUR", null, null)));

        adminService.createUser(ADMIN, createRequest("EMPLOYEUR"));

        verify(userAccountRepository).create(any(), any(), any(), any(), eq("hash"), eq("EMPLOYEUR"), eq(false), eq(ADMIN.id()));
    }

    @Test
    void adminCannotManageAnotherAdminAccount() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(account(id, "ADMIN", null, null)));

        assertThatThrownBy(() -> adminService.updateUserArchived(ADMIN, id, true))
                .isInstanceOf(ForbiddenException.class);
        verify(userAccountRepository, never()).updateArchived(any(), anyBoolean());
    }

    @Test
    void superAdminManagesTheAdminsHeCreated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(adminWithDossier(id, "Rue 10, Medina")));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(SUPER_ADMIN.id()));

        adminService.updateUserArchived(SUPER_ADMIN, id, true);
        adminService.updateUserActive(SUPER_ADMIN, id, true);

        verify(userAccountRepository).updateArchived(id, true);
        verify(userAccountRepository).updateActive(id, true);
    }

    @Test
    void superAdminCannotManageAdminCreatedByAnotherSuperAdmin() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(account(id, "ADMIN", null, null)));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> adminService.updateUserActive(SUPER_ADMIN, id, false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("que tu as crees");
        verify(userAccountRepository, never()).updateActive(any(), anyBoolean());
    }

    @Test
    void superAdminAccountIsNeverEditable() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(account(id, "SUPER_ADMIN", null, null)));

        assertThatThrownBy(() -> adminService.updateUserArchived(SUPER_ADMIN, id, true))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> adminService.updateUserActive(ADMIN, id, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminSeesOnlyTheAccountsHeCreated() {
        when(userAccountRepository.findAllCreatedBy(ADMIN.id())).thenReturn(List.of(
                account(UUID.randomUUID(), "EMPLOYEUR", null, null),
                account(UUID.randomUUID(), "PRESTATAIRE", null, null)
        ));
        when(userAccountRepository.findAll()).thenReturn(List.of(
                account(UUID.randomUUID(), "SUPER_ADMIN", null, null),
                account(UUID.randomUUID(), "ADMIN", null, null),
                account(UUID.randomUUID(), "EMPLOYEUR", null, null),
                account(UUID.randomUUID(), "EMPLOYEUR", null, null),
                account(UUID.randomUUID(), "PRESTATAIRE", null, null)
        ));

        assertThat(adminService.users(ADMIN)).extracting(UserResponse::role).containsExactly("EMPLOYEUR", "PRESTATAIRE");
        assertThat(adminService.users(SUPER_ADMIN)).hasSize(5);
    }

    @Test
    void adminCannotManageAccountCreatedByAnotherAdmin() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(employer(id, "/front", "/back")));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> adminService.updateUserActive(ADMIN, id, true))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("que tu as crees");
        assertThatThrownBy(() -> adminService.assertCanViewUser(ADMIN, id))
                .isInstanceOf(ForbiddenException.class);
        verify(userAccountRepository, never()).updateActive(any(), anyBoolean());
    }

    @Test
    void adminCannotManageSelfRegisteredAccount() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(employer(id, "/front", "/back")));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserArchived(ADMIN, id, true))
                .isInstanceOf(ForbiddenException.class);
        // Le super administrateur gere tous les comptes, y compris les inscriptions libres.
        adminService.updateUserArchived(SUPER_ADMIN, id, true);
        verify(userAccountRepository).updateArchived(id, true);
    }

    @Test
    void adminSeesOnlyBookingsOfHisAccountsAndCannotUpdateOthers() {
        UUID bookingId = UUID.randomUUID();
        when(bookingAdminRepository.findAllManagedBy(ADMIN.id(), null)).thenReturn(List.of());
        when(bookingAdminRepository.findStatus(bookingId)).thenReturn(Optional.of("REQUESTED"));
        when(bookingAdminRepository.isManagedBy(bookingId, ADMIN.id())).thenReturn(false);

        assertThat(adminService.bookings(ADMIN, null)).isEmpty();
        assertThatThrownBy(() -> adminService.updateBookingStatus(ADMIN, bookingId, "ACCEPTED"))
                .isInstanceOf(ForbiddenException.class);
        verify(bookingAdminRepository, never()).findAll(any());
        verify(bookingAdminRepository, never()).updateStatus(any(), any(), any());
    }

    @Test
    void adminWithoutAddressCannotBeValidated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(adminWithDossier(id, null)));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(SUPER_ADMIN.id()));

        assertThatThrownBy(() -> adminService.updateUserActive(SUPER_ADMIN, id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adresse")
                .hasMessageNotContaining("photo du profil");
        verify(userAccountRepository, never()).updateActive(any(), anyBoolean());
    }

    @Test
    void adminWithoutIdentityDocumentCannotBeValidated() {
        UUID id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(account(id, "ADMIN", null, null)));
        when(userAccountRepository.findCreatedBy(id)).thenReturn(Optional.of(SUPER_ADMIN.id()));

        assertThatThrownBy(() -> adminService.updateUserActive(SUPER_ADMIN, id, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recto de la piece d'identite")
                .hasMessageContaining("verso de la piece d'identite")
                .hasMessageContaining("localisation");
    }

    private UserResponse adminWithDossier(UUID id, String address) {
        return new UserResponse(
                id, "Admin Secondaire", "admin2@jaankha.test", "+221771234567", "ADMIN",
                false, false, false,
                null, "/front", "/back",
                null, "Dakar", null, 0, List.of(), null, null, "2026-11-01T10:00:00Z", address
        );
    }

    private AdminCreateUserRequest createRequest(String role) {
        return new AdminCreateUserRequest("Nouveau Compte", "nouveau@jaankha.test", "+221771234567", "Secret1!", role);
    }

    private UserResponse account(UUID id, String role, String identityFront, String identityBack) {
        return new UserResponse(
                id, "Compte " + role, "compte@jaankha.test", "+221771234567", role,
                false, false, false,
                null, identityFront, identityBack,
                null, null, null, 0, List.of(), null, null, "2026-11-01T10:00:00Z", null
        );
    }

    private UserResponse employer(UUID id, String identityFront, String identityBack) {
        return new UserResponse(
                id, "Moussa Fall", "moussa@jaankha.test", "+221771234567", "EMPLOYEUR",
                false, false, false,
                "http://localhost/uploads/profiles/photo.jpg", identityFront, identityBack,
                null, "Dakar", null, 0, List.of(), null, null, "2026-11-01T10:00:00Z", null
        );
    }
}
