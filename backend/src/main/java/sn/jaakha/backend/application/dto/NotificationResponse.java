package sn.jaakha.backend.application.dto;

import java.time.Instant;
import java.util.UUID;
import sn.jaakha.backend.domain.enums.NotificationType;
import sn.jaakha.backend.domain.model.Notification;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID bookingId,
        UUID userId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getBookingId(),
                notification.getUserId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
