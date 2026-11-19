package sn.jaakha.backend.application.dto;

import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        UUID bookingId,
        UUID userId,
        boolean read,
        String createdAt
) {
}
