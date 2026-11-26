package sn.jaakha.backend.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test d'integration (H2) du compte initial : il est cree avec le role SUPER_ADMIN, et un ancien
 * compte initial ADMIN est promu super administrateur s'il n'en existe pas encore.
 */
@SpringBootTest
class SuperAdminBootstrapTest {
    private final UserAccountSchemaInitializer initializer = new UserAccountSchemaInitializer();

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from app_user where email like 'bootstrap-%@jaankha.test'");
    }

    @Test
    void initialAccountIsCreatedAsSuperAdmin() {
        initializer.ensureAdminAccount(jdbcTemplate, passwordEncoder, "bootstrap-new@jaankha.test", "+221770000001", "Secret1!");

        assertThat(roleOf("bootstrap-new@jaankha.test")).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void legacyInitialAdminIsPromotedToSuperAdmin() {
        jdbcTemplate.update("""
                        insert into app_user (id, full_name, email, phone, password_hash, role,
                                              email_verified, active, archived, created_at, updated_at)
                        values (?, 'Admin Jaankha', 'bootstrap-old@jaankha.test', '+221770000002', 'hash', 'ADMIN',
                                true, true, false, current_timestamp, current_timestamp)
                        """,
                UUID.randomUUID());

        initializer.ensureAdminAccount(jdbcTemplate, passwordEncoder, "bootstrap-old@jaankha.test", "+221770000002", "Secret1!");

        assertThat(roleOf("bootstrap-old@jaankha.test")).isEqualTo("SUPER_ADMIN");
    }

    private String roleOf(String email) {
        return jdbcTemplate.queryForObject("select role from app_user where email = ?", String.class, email);
    }
}
