package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.EmailOtp;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {
    List<EmailOtp> findByUserAndUsedFalseOrderByCreatedAtDesc(AppUser user);
}
