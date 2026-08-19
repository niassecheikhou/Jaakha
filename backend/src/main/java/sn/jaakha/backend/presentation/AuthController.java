package sn.jaakha.backend.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.AuthService;
import sn.jaakha.backend.application.dto.AuthResponse;
import sn.jaakha.backend.application.dto.LoginRequest;
import sn.jaakha.backend.application.dto.OtpRequest;
import sn.jaakha.backend.application.dto.RegisterRequest;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.application.dto.VerifyOtpRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/request-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestOtp(request);
    }

    @PostMapping("/verify-email")
    public UserResponse verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyEmail(request);
    }
}
