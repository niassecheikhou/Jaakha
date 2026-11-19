package sn.jaakha.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.model.AccountProfile;

public interface AccountProfileRepository extends JpaRepository<AccountProfile, UUID> {
    Optional<AccountProfile> findByOwnerKey(String ownerKey);
}
