package sn.jaakha.backend.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAccountSchemaInitializerTest {
    private final UserAccountSchemaInitializer initializer = new UserAccountSchemaInitializer();
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void refusesToCreateAdminWithoutPassword() {
        assertThatThrownBy(() -> initializer.ensureAdminAccount(
                jdbcTemplate, passwordEncoder, "admin@jaankha.test", "+221770000000", ""
        )).isInstanceOf(IllegalStateException.class);

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skipsAdminCreationWhenNoAdminEmail() {
        initializer.ensureAdminAccount(jdbcTemplate, passwordEncoder, "", "+221770000000", "");

        verifyNoInteractions(jdbcTemplate, passwordEncoder);
    }
}
