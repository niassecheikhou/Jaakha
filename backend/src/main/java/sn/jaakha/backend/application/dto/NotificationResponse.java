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
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getBookingId(),
                notification.getUserId(),
