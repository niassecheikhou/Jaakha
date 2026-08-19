package sn.jaakha.backend.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import sn.jaakha.backend.domain.enums.BookingStatus;
import sn.jaakha.backend.domain.model.Booking;

public record BookingResponse(
        UUID id,
        BookingStatus status,
        LocalDate serviceDate,
        LocalTime startTime,
        int durationHours,
        String address,
        String instructions,
        UUID employerId,
        String employerName,
        UUID providerId,
        String providerName,
        Instant createdAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getServiceDate(),
                booking.getStartTime(),
                booking.getDurationHours(),
                booking.getAddress(),
                booking.getInstructions(),
                booking.getEmployer().getId(),
                booking.getEmployer().getFullName(),
                booking.getProvider().getId(),
                booking.getProvider().getUser().getFullName(),
                booking.getCreatedAt()
        );
    }
}
