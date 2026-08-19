package sn.jaakha.backend.application;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AuthResponse;
import sn.jaakha.backend.application.dto.LoginRequest;
import sn.jaakha.backend.application.dto.OtpRequest;
import sn.jaakha.backend.application.dto.RegisterRequest;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.application.dto.VerifyOtpRequest;
import sn.jaakha.backend.domain.enums.NotificationType;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.exception.FieldValidationException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.EmailOtp;
import sn.jaakha.backend.domain.model.SessionToken;
import sn.jaakha.backend.domain.repository.AppUserRepository;
import sn.jaakha.backend.domain.repository.EmailOtpRepository;
import sn.jaakha.backend.domain.repository.SessionTokenRepository;
import sn.jaakha.backend.infrastructure.mail.MailService;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern SENEGAL_PHONE_PATTERN = Pattern.compile("^\\+221(70|71|76|77|78)[0-9]{7}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$");

    private final AppUserRepository userRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();
    private final long tokenTtlHours;

    public AuthService(AppUserRepository userRepository, SessionTokenRepository sessionTokenRepository,
            EmailOtpRepository emailOtpRepository, PasswordEncoder passwordEncoder, MailService mailService,
            NotificationService notificationService, @Value("${app.auth.token-ttl-hours}") long tokenTtlHours) {
        this.userRepository = userRepository;
        this.sessionTokenRepository = sessionTokenRepository;
        this.emailOtpRepository = emailOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.tokenTtlHours = tokenTtlHours;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegistrationFields(request.fullName(), request.email(), request.phone(), request.password());
        if (request.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La creation admin publique est interdite.");
        }
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());
        ensureUnique(email, phone);

        AppUser user = userRepository.save(new AppUser(
                request.fullName().trim(),
                email,
                phone,
                passwordEncoder.encode(request.password()),
                request.role()
        ));

        if (email != null) {
            sendOtp(user);
        }
        if (request.role() == Role.PRESTATAIRE) {
            notificationService.notifyAdmins(
                    NotificationType.PROVIDER_PENDING_VALIDATION,
                    "Nouveau prestataire a verifier",
                    user.getFullName() + " vient de creer un compte prestataire.",
                    null,
                    user.getId()
            );
        }
        return new AuthResponse(createToken(user), UserResponse.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = findByIdentifier(request.identifier())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants invalides."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants invalides.");
        }
        return new AuthResponse(createToken(user), UserResponse.from(user));
    }

    @Transactional
    public void requestOtp(OtpRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        sendOtp(user);
    }

    @Transactional
    public UserResponse verifyEmail(VerifyOtpRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        EmailOtp otp = emailOtpRepository.findByUserAndUsedFalseOrderByCreatedAtDesc(user).stream()
                .filter(candidate -> candidate.getExpiresAt().isAfter(Instant.now()))
                .filter(candidate -> passwordEncoder.matches(request.code(), candidate.getCodeHash()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Code OTP invalide ou expire."));
        otp.setUsed(true);
        user.setEmailVerified(true);
        return UserResponse.from(userRepository.save(user));
    }

    public AppUser createUserByAdmin(String fullName, String email, String phone, String rawPassword, Role role) {
        validateRegistrationFields(fullName, email, phone, rawPassword);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        ensureUnique(normalizedEmail, normalizedPhone);
        AppUser user = userRepository.save(new AppUser(
                fullName.trim(),
                normalizedEmail,
                normalizedPhone,
                passwordEncoder.encode(rawPassword),
                role
        ));
        if (normalizedEmail != null) {
            mailService.send(
                    normalizedEmail,
                    "Vos identifiants Jaakha",
                    "Votre compte Jaakha est pret. Telephone: " + normalizedPhone + " Mot de passe: " + rawPassword
            );
        }
        return user;
    }

    private void sendOtp(AppUser user) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        emailOtpRepository.save(new EmailOtp(user, passwordEncoder.encode(code), Instant.now().plus(15, ChronoUnit.MINUTES)));
        mailService.send(user.getEmail(), "Code de verification Jaakha", "Votre code de verification est: " + code);
    }

    private String createToken(AppUser user) {
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        sessionTokenRepository.save(new SessionToken(token, user, Instant.now().plus(tokenTtlHours, ChronoUnit.HOURS)));
        return token;
    }

    private java.util.Optional<AppUser> findByIdentifier(String identifier) {
        String normalized = identifier.trim();
        if (normalized.contains("@")) {
            return userRepository.findByEmailIgnoreCase(normalizeEmail(normalized));
        }
        return userRepository.findByPhone(normalized);
    }

    private void ensureUnique(String email, String phone) {
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email deja utilise.");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Telephone deja utilise.");
        }
    }

    private void validateRegistrationFields(String fullName, String email, String phone, String password) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (fullName == null || fullName.isBlank()) {
            fields.put("fullName", "Le nom complet est obligatoire.");
        } else if (fullName.trim().length() < 5) {
            fields.put("fullName", "Le nom complet doit contenir au moins 5 caracteres.");
        }
        if (email == null || email.isBlank()) {
            fields.put("email", "L'adresse email est obligatoire.");
        } else if (!EMAIL_PATTERN.matcher(email.trim().toLowerCase(Locale.ROOT)).matches()) {
            fields.put("email", "L'adresse email doit etre valide, par exemple cheikh@gmail.com.");
        }
        if (phone == null || phone.isBlank()) {
            fields.put("phone", "Le numero de telephone est obligatoire.");
        } else if (!SENEGAL_PHONE_PATTERN.matcher(phone.trim()).matches()) {
            fields.put("phone", "Le numero de telephone doit etre un numero senegalais valide au format +221 suivi de 70, 71, 76, 77 ou 78. Exemple: +221771234567.");
        }
        if (password == null || password.isBlank()) {
            fields.put("password", "Le mot de passe est obligatoire.");
        } else if (password.length() < 6) {
            fields.put("password", "Le mot de passe doit contenir au moins 6 caracteres.");
        } else if (!STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            fields.put("password", "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractere special.");
        }
        if (!fields.isEmpty()) {
            throw new FieldValidationException(fields);
        }
    }

    private String normalizeEmail(String email) {
        return blankToNull(email) == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
