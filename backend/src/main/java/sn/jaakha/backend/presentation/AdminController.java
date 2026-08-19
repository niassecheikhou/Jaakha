package sn.jaakha.backend.presentation;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.AdminService;
import sn.jaakha.backend.application.BookingService;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.ReportService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.BookingStatusRequest;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.application.dto.ProviderStatusRequest;
import sn.jaakha.backend.application.dto.ReportResponse;
import sn.jaakha.backend.application.dto.ReportStatusRequest;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.enums.BookingStatus;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.enums.ReportStatus;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserAccess userAccess;
    private final AdminService adminService;
    private final ProviderService providerService;
    private final BookingService bookingService;
    private final ReportService reportService;

    public AdminController(UserAccess userAccess, AdminService adminService, ProviderService providerService,
            BookingService bookingService, ReportService reportService) {
        this.userAccess = userAccess;
        this.adminService = adminService;
        this.providerService = providerService;
        this.bookingService = bookingService;
        this.reportService = reportService;
    }

    @GetMapping("/users")
    public List<UserResponse> users(@AuthenticationPrincipal JaakhaUserPrincipal principal) {
        userAccess.requireRole(principal, Role.ADMIN);
        return adminService.listUsers();
    }

    @PostMapping("/users")
    public UserResponse createUser(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @Valid @RequestBody AdminCreateUserRequest request) {
        userAccess.requireRole(principal, Role.ADMIN);
        return adminService.createUser(request);
    }

    @PatchMapping("/users/{id}/active")
    public UserResponse setActive(@AuthenticationPrincipal JaakhaUserPrincipal principal, @PathVariable UUID id,
            @RequestParam boolean active) {
        userAccess.requireRole(principal, Role.ADMIN);
        return adminService.setActive(id, active);
    }

    @GetMapping("/providers")
    public List<ProviderProfileResponse> providers(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @RequestParam(required = false) ProviderStatus status) {
        userAccess.requireRole(principal, Role.ADMIN);
        return providerService.listAll(status);
    }

    @PatchMapping("/providers/{id}/status")
    public ProviderProfileResponse updateProviderStatus(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @PathVariable UUID id, @Valid @RequestBody ProviderStatusRequest request) {
        userAccess.requireRole(principal, Role.ADMIN);
        return providerService.updateStatus(id, request.status());
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @RequestParam(required = false) BookingStatus status) {
        userAccess.requireRole(principal, Role.ADMIN);
        return bookingService.listAll(status);
    }

    @PatchMapping("/bookings/{id}/status")
    public BookingResponse updateBookingStatus(@AuthenticationPrincipal JaakhaUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.updateStatus(userAccess.requireRole(principal, Role.ADMIN), id, request.status());
    }

    @GetMapping("/reports")
    public List<ReportResponse> reports(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @RequestParam(required = false) ReportStatus status) {
        userAccess.requireRole(principal, Role.ADMIN);
        return reportService.listAll(status);
    }

    @PatchMapping("/reports/{id}/status")
    public ReportResponse updateReportStatus(@AuthenticationPrincipal JaakhaUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody ReportStatusRequest request) {
        userAccess.requireRole(principal, Role.ADMIN);
        return reportService.updateStatus(id, request.status());
    }
}
