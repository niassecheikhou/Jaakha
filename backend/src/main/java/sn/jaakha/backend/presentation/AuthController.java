package sn.jaakha.backend.presentation;

import sn.jaakha.backend.application.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.AuthService;
import sn.jaakha.backend.application.dto.AuthResponse;
import sn.jaakha.backend.application.dto.ForgotPasswordRequest;
import sn.jaakha.backend.application.dto.LoginRequest;
import sn.jaakha.backend.application.dto.MessageResponse;
import sn.jaakha.backend.application.dto.RegisterRequest;
import sn.jaakha.backend.application.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    /** Compte connecte, relu en base : permet au front de connaitre le role a jour (ex. promotion SUPER_ADMIN). */
    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        return authService.currentUser(currentUserService.requireUser(request).id());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        auditLogService.record(response.user(), AuditLogService.LOGIN, "COMPTE", response.user().id(), response.user().fullName(), null);
        return response;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        auditLogService.record(response.user(), AuditLogService.REGISTER, "COMPTE", response.user().id(), response.user().fullName(),
                "Inscription en tant que " + response.user().role());
        return response;
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
