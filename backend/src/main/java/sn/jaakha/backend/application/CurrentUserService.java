package sn.jaakha.backend.application;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.presentation.ForbiddenException;
import sn.jaakha.backend.presentation.UnauthorizedException;

@Service
public class CurrentUserService {
    private final JdbcTemplate jdbcTemplate;

    public CurrentUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthenticatedUser requireUser(HttpServletRequest request) {
        String token = bearerToken(request);
        if (token == null) {
            throw new UnauthorizedException("Tu dois etre connecte pour effectuer cette action.");
        }

        return jdbcTemplate.query("""
                        select u.id, u.full_name, u.role
                        from session_token st
                        join app_user u on u.id = st.user_id
                        where st.token = ?
                          and st.expires_at > current_timestamp
                          and u.archived = false
                        """,
                preparedStatement -> preparedStatement.setString(1, token),
                resultSet -> {
                    if (!resultSet.next()) {
                        throw new UnauthorizedException("Ta session est invalide ou expiree. Connecte-toi puis reessaie.");
                    }
                    return mapUser(resultSet);
                });
    }

    public String ownerKeyOrLocal(HttpServletRequest request) {
        String token = bearerToken(request);
        if (token == null) {
            return "local-account";
        }
        try {
            return requireUser(request).id().toString();
        } catch (UnauthorizedException exception) {
            return token;
        }
    }

    public AuthenticatedUser requireAdmin(HttpServletRequest request) {
        AuthenticatedUser user = requireUser(request);
        if (!"ADMIN".equals(user.role())) {
            throw new ForbiddenException("Tu dois etre connecte avec un compte administrateur pour effectuer cette action.");
        }
        return user;
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return authorization.replaceFirst("(?i)^Bearer\\s+", "").trim();
    }

    private AuthenticatedUser mapUser(ResultSet resultSet) throws SQLException {
        return new AuthenticatedUser(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("full_name"),
                resultSet.getString("role")
        );
    }
}
