package sn.jaakha.backend.presentation;

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

import sn.jaakha.backend.application.BookingService;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.ReportService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.BookingStatusRequest;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final AccountProfileService accountProfileService;
    private final CurrentUserService currentUserService;

    public AdminController(
            AdminService adminService,
            AccountProfileService accountProfileService,
            CurrentUserService currentUserService
    ) {
        this.adminService = adminService;
        this.accountProfileService = accountProfileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users")
    public List<UserResponse> users(HttpServletRequest request) {
        currentUserService.requireAdmin(request);
        return adminService.users();
    }

    @GetMapping("/users/{id}/identity-photo")
    public ResponseEntity<Resource> userIdentityPhoto(HttpServletRequest request, @PathVariable UUID id) {
        currentUserService.requireAdmin(request);
        return AccountProfileController.privateImage(accountProfileService.identityPhoto(id.toString()));
    }

    @PostMapping("/users")
    public UserResponse createUser(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        currentUserService.requireAdmin(httpRequest);
        return adminService.createUser(request);
    }

    @PatchMapping("/users/{id}/active")

            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @Valid @RequestBody AccountStatusUpdateRequest request
    ) {
        currentUserService.requireAdmin(httpRequest);
        return adminService.updateUserActive(id, request.active());
    }

    @PatchMapping("/users/{id}/archive")
    public UserResponse updateUserArchived(
            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @Valid @RequestBody AccountArchiveRequest request
    ) {
        currentUserService.requireAdmin(httpRequest);
        return adminService.updateUserArchived(id, request.archived());
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
        currentUserService.requireAdmin(httpRequest);
        return adminService.updateProviderStatus(id, request.status());
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
        currentUserService.requireAdmin(httpRequest);
        return adminService.updateBookingStatus(id, request.status());

}
