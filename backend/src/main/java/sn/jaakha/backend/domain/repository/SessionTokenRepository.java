package sn.jaakha.backend.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.model.SessionToken;

public interface SessionTokenRepository extends JpaRepository<SessionToken, UUID> {
    Optional<SessionToken> findByTokenAndExpiresAtAfter(String token, Instant now);
}
