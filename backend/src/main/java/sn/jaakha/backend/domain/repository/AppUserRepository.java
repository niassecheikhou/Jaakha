package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByPhone(String phone);

    List<AppUser> findByRole(Role role);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}
