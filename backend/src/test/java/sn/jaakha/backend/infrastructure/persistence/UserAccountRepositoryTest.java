package sn.jaakha.backend.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sn.jaakha.backend.application.dto.UserResponse;

/**
 * Test d'integration sur H2 en memoire : la date d'inscription est exposee a l'administration
 * (elle sert a filtrer les indicateurs du dashboard par periode).
 */
@SpringBootTest
class UserAccountRepositoryTest {
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void createdUserExposesItsRegistrationDate() {
        UUID id = UUID.randomUUID();
        String suffix = id.toString().substring(0, 7).replaceAll("[^0-9]", "1");
        userAccountRepository.create(
                id, "Test Periode", "periode-" + id + "@jaankha.test",
                "+22177" + String.format("%7s", suffix).replace(' ', '0').substring(0, 7),
                "hash", "EMPLOYEUR", false
        );

        UserResponse user = userAccountRepository.findById(id).orElseThrow();

        assertThat(user.createdAt()).isNotNull();
        Instant createdAt = Instant.parse(user.createdAt());
        assertThat(Duration.between(createdAt, Instant.now()).abs()).isLessThan(Duration.ofHours(25));
        assertThat(userAccountRepository.findAll())
                .filteredOn(account -> account.id().equals(id))
                .singleElement()
                .extracting(UserResponse::createdAt)
                .isNotNull();
    }
}
