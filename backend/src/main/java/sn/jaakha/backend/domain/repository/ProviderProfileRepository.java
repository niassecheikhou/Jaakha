package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderProfile;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {
    Optional<ProviderProfile> findByOwnerKey(String ownerKey);

    Optional<ProviderProfile> findByUserId(UUID userId);

    List<ProviderProfile> findByStatusOrderByCreatedAtDesc(ProviderStatus status);
}
