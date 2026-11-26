package sn.jaakha.backend.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jaakha.backend.application.dto.AuthResponse;
import sn.jaakha.backend.application.dto.ForgotPasswordRequest;
import sn.jaakha.backend.application.dto.LoginRequest;
import sn.jaakha.backend.application.dto.MessageResponse;
import sn.jaakha.backend.application.dto.RegisterRequest;
import sn.jaakha.backend.application.dto.ResetPasswordRequest;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.presentation.ConflictException;
import sn.jaakha.backend.presentation.UnauthorizedException;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;
    private final long resetTokenTtlMinutes;
    private final long tokenTtlHours;

    public AuthService(
            JdbcTemplate jdbcTemplate,
            MailService mailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend-url:${APP_FRONTEND_URL:http://localhost:5173}}") String frontendUrl,
            @Value("${app.auth.reset-token-ttl-minutes:${RESET_TOKEN_TTL_MINUTES:30}}") long resetTokenTtlMinutes,
            @Value("${app.auth.token-ttl-hours:${AUTH_TOKEN_TTL_HOURS:720}}") long tokenTtlHours
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
        this.resetTokenTtlMinutes = resetTokenTtlMinutes;
        this.tokenTtlHours = tokenTtlHours;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserWithPassword user = findByIdentifier(request.identifier())
                .stream()
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Aucun compte ne correspond a cet email ou telephone, ou ce compte est archive."));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new UnauthorizedException("Mot de passe incorrect.");
        }

        return new AuthResponse(createToken(user.id()), user.toResponse());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UUID userId = UUID.randomUUID();
        String role = request.role() == null || request.role().isBlank() ? "EMPLOYEUR" : request.role();
        try {
            jdbcTemplate.update("""
                            insert into app_user (
                                id, full_name, email, phone, password_hash, role,
                                email_verified, active, archived, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, false, false, false, current_timestamp, current_timestamp)
                            """,
                    userId,
                    request.fullName().trim(),
                    request.email().trim().toLowerCase(Locale.ROOT),
                    request.phone().trim(),
                    passwordEncoder.encode(request.password()),
                    role
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Un compte existe deja avec cet email ou ce numero de telephone.");
        }

        UserResponse user = findById(userId);
        mailService.sendAccountCreated(user, null);
        return new AuthResponse(createToken(user.id()), user);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        findByIdentifier(request.identifier()).stream().findFirst().ifPresent(user ->
                mailService.sendPasswordResetLink(user.toResponse(), issuePasswordResetUrl(user.id()))
        );

        return new MessageResponse("Si un compte correspond a ces informations, un email de reinitialisation a ete envoye.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token());
        ResetToken resetToken = jdbcTemplate.query("""
                        select prt.id, prt.user_id
                        from password_reset_token prt
                        join app_user u on u.id = prt.user_id
                        where prt.token_hash = ?
                          and prt.used_at is null
                          and prt.expires_at > current_timestamp
                          and u.archived = false
                        """,
                (resultSet, rowNumber) -> new ResetToken(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("user_id", UUID.class)
                ),
                tokenHash
        ).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ce lien de reinitialisation est invalide ou expire."));

        jdbcTemplate.update(
                "update app_user set password_hash = ?, updated_at = current_timestamp where id = ?",
                passwordEncoder.encode(request.password()),
                resetToken.userId()
        );
        jdbcTemplate.update(
                "update password_reset_token set used_at = current_timestamp where id = ?",
                resetToken.id()
        );
        jdbcTemplate.update("delete from session_token where user_id = ?", resetToken.userId());

        return new MessageResponse("Mot de passe reinitialise avec succes. Vous pouvez maintenant vous connecter.");
    }

    private List<UserWithPassword> findByIdentifier(String identifier) {
        String cleanIdentifier = identifier.trim().toLowerCase(Locale.ROOT);
        return jdbcTemplate.query("""
                        select id, full_name, email, phone, role, email_verified, active, archived, created_at, password_hash
                        from app_user
                        where archived = false and (lower(email) = ? or phone = ?)
                        """,
                (resultSet, rowNumber) -> mapUserWithPassword(resultSet),
                cleanIdentifier,
                identifier.trim()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(UUID userId) {
        return findById(userId);
    }

    private UserResponse findById(UUID userId) {
        return jdbcTemplate.queryForObject("""
                        select id, full_name, email, phone, role, email_verified, active, archived, created_at
                        from app_user
                        where id = ?
                        """,
                (resultSet, rowNumber) -> mapUser(resultSet),
                userId
        );
    }

    private String createToken(UUID userId) {
        UUID tokenId = UUID.randomUUID();
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(tokenTtlHours, ChronoUnit.HOURS);
        jdbcTemplate.update("""
                        insert into session_token (id, token, user_id, expires_at, created_at)
                        values (?, ?, ?, ?, current_timestamp)
                        """,
                tokenId,
                token,
                userId,
                Timestamp.from(expiresAt)
        );
        return token;
    }

    /**
     * Cree un jeton de reinitialisation et retourne le lien du front pour choisir un mot de passe.
     */
    @Transactional
    public String issuePasswordResetUrl(UUID userId) {
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(resetTokenTtlMinutes, ChronoUnit.MINUTES);
        jdbcTemplate.update("""
                        insert into password_reset_token (
                            id, user_id, token_hash, expires_at, created_at
                        )
                        values (?, ?, ?, ?, current_timestamp)
                        """,
                UUID.randomUUID(),
                userId,
                hashToken(token),
                Timestamp.from(expiresAt)
        );
        return resetUrl(token);
    }

    private String resetUrl(String token) {
        String cleanFrontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        return cleanFrontendUrl + "/reset-password?token=" + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible pour les jetons de reinitialisation.", exception);
        }
    }

    private UserWithPassword mapUserWithPassword(ResultSet resultSet) throws SQLException {
        UserResponse response = mapUser(resultSet);
        return new UserWithPassword(response, resultSet.getString("password_hash"));
    }

    private UserResponse mapUser(ResultSet resultSet) throws SQLException {
        return new UserResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("role"),
                resultSet.getBoolean("email_verified"),
                resultSet.getBoolean("active"),
                resultSet.getBoolean("archived"),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                List.of(),
                null,
                null,
                createdAt(resultSet)
        );
    }

    private String createdAt(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return createdAt == null ? null : createdAt.toInstant().toString();
    }

    private record UserWithPassword(UserResponse response, String passwordHash) {
        UUID id() {
            return response.id();
        }

        UserResponse toResponse() {
            return response;
        }
    }

    private record ResetToken(UUID id, UUID userId) {
    }
}
