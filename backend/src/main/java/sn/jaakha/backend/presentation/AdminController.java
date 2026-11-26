package sn.jaakha.backend.presentation;

import sn.jaakha.backend.application.dto.AuditLogResponse;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import sn.jaakha.backend.application.AccountProfileService;
import sn.jaakha.backend.application.AdminService;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.dto.AccountArchiveRequest;
import sn.jaakha.backend.application.dto.AccountStatusUpdateRequest;
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.application.dto.StatusUpdateRequest;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.enums.IdentityDocumentSide;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuditLogService auditLogService;
    private final AdminService adminService;
    private final AccountProfileService accountProfileService;
    private final CurrentUserService currentUserService;

    public AdminController(
            AdminService adminService,
            AccountProfileService accountProfileService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.adminService = adminService;
        this.accountProfileService = accountProfileService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public List<UserResponse> users(HttpServletRequest request) {
        return adminService.users(currentUserService.requireAdmin(request));
    }

    @GetMapping("/users/{id}/identity-photo")
    public ResponseEntity<Resource> userIdentityPhoto(HttpServletRequest request, @PathVariable UUID id) {
        currentUserService.requireAdmin(request);
        return AccountProfileController.privateImage(accountProfileService.identityPhoto(id.toString(), IdentityDocumentSide.FRONT));
    }

    @GetMapping("/users/{id}/identity-photo/back")
    public ResponseEntity<Resource> userIdentityPhotoBack(HttpServletRequest request, @PathVariable UUID id) {
        currentUserService.requireAdmin(request);
        return AccountProfileController.privateImage(accountProfileService.identityPhoto(id.toString(), IdentityDocumentSide.BACK));
    }

    @PostMapping("/users")
    public UserResponse createUser(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        AuthenticatedUser actor = currentUserService.requireAdmin(httpRequest);
        UserResponse user = adminService.createUser(actor, request);
        auditLogService.record(actor, AuditLogService.ACCOUNT_CREATED, "COMPTE", user.id(), user.fullName(), "Role : " + user.role());
        return user;
    }

    @PatchMapping("/users/{id}/active")
    public UserResponse updateUserActive(
            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @Valid @RequestBody AccountStatusUpdateRequest request
    ) {
        AuthenticatedUser actor = currentUserService.requireAdmin(httpRequest);
        UserResponse user = adminService.updateUserActive(actor, id, request.active());
        auditLogService.record(actor, request.active() ? AuditLogService.ACCOUNT_VALIDATED : AuditLogService.ACCOUNT_INVALIDATED,
                "COMPTE", user.id(), user.fullName(), "Role : " + user.role());
        return user;
    }

    @PatchMapping("/users/{id}/archive")
    public UserResponse updateUserArchived(
            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @Valid @RequestBody AccountArchiveRequest request
    ) {
        AuthenticatedUser actor = currentUserService.requireAdmin(httpRequest);
        UserResponse user = adminService.updateUserArchived(actor, id, request.archived());
        auditLogService.record(actor, request.archived() ? AuditLogService.ACCOUNT_ARCHIVED : AuditLogService.ACCOUNT_RESTORED,
                "COMPTE", user.id(), user.fullName(), "Role : " + user.role());
        return user;
    }

    /**
     * Historique des actions (reserve au super administrateur). Filtres optionnels : periode [from, to[
     * en ISO-8601, auteur, role de l'auteur et type d'action.
     */
    @GetMapping("/audit-logs")
    public List<AuditLogResponse> auditLogs(
            HttpServletRequest request,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String action
    ) {
        currentUserService.requireSuperAdmin(request);
        return auditLogService.search(from, to, actorId, actorRole, action);
    }

    @GetMapping("/providers")
    public List<ProviderProfileResponse> providers(HttpServletRequest request, @RequestParam(required = false) String status) {
        currentUserService.requireAdmin(request);
        return adminService.providers(status);
    }

    @PatchMapping("/providers/{id}/status")
    public ProviderProfileResponse updateProviderStatus(
            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        AuthenticatedUser actor = currentUserService.requireAdmin(httpRequest);
        ProviderProfileResponse profile = adminService.updateProviderStatus(id, request.status());
        auditLogService.record(actor, AuditLogService.PROVIDER_STATUS_CHANGED, "PROFIL_PRESTATAIRE", profile.id(), profile.fullName(),
                "APPROVED".equals(request.status()) ? "Profil valide" : "Nouveau statut : " + request.status());
        return profile;
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings(HttpServletRequest request, @RequestParam(required = false) String status) {
        currentUserService.requireAdmin(request);
        return adminService.bookings(status);
    }

    @PatchMapping("/bookings/{id}/status")
    public BookingResponse updateBookingStatus(
            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        AuthenticatedUser actor = currentUserService.requireAdmin(httpRequest);
        BookingResponse booking = adminService.updateBookingStatus(id, request.status());
        auditLogService.record(actor, AuditLogService.BOOKING_STATUS_CHANGED, "RESERVATION", booking.id(),
                booking.employerName() + " / " + booking.providerName(), "Nouveau statut : " + AuditLogService.bookingStatusLabel(booking.status()));
        return booking;
    }
}
