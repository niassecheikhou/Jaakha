package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient);
}
