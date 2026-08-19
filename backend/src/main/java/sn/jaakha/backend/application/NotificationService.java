package sn.jaakha.backend.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.NotificationResponse;
import sn.jaakha.backend.domain.enums.NotificationType;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.Notification;
import sn.jaakha.backend.domain.repository.AppUserRepository;
import sn.jaakha.backend.domain.repository.NotificationRepository;
import sn.jaakha.backend.infrastructure.mail.MailService;

@Service
public class NotificationService {
    private final AppUserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final MailService mailService;

    public NotificationService(AppUserRepository userRepository, NotificationRepository notificationRepository,
            MailService mailService) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.mailService = mailService;
    }

    public void notifyAdmins(NotificationType type, String title, String message, UUID bookingId, UUID userId) {
        userRepository.findByRole(Role.ADMIN).stream()
                .filter(AppUser::isActive)
                .forEach(admin -> {
                    notificationRepository.save(new Notification(admin, type, title, message, bookingId, userId));
                    mailService.send(admin.getEmail(), title, message);
                });
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listFor(AppUser user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(AppUser user, UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification introuvable."));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
