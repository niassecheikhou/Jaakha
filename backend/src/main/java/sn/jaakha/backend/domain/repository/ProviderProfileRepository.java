package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.ProviderProfile;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {
    Optional<ProviderProfile> findByUser(AppUser user);

    List<ProviderProfile> findByStatusOrderByCreatedAtDesc(ProviderStatus status);

    List<ProviderProfile> findAllByOrderByCreatedAtDesc();
}
