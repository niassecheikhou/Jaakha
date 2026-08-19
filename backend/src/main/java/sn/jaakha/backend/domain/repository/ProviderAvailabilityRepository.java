package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.model.ProviderAvailability;
import sn.jaakha.backend.domain.model.ProviderProfile;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, UUID> {
    List<ProviderAvailability> findByProvider(ProviderProfile provider);

    void deleteByProvider(ProviderProfile provider);
}
