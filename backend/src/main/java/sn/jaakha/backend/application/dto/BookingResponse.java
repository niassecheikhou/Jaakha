package sn.jaakha.backend.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String status,
        LocalDate serviceDate,
        LocalTime startTime,
        int durationHours,
        String address,
        String instructions,
        UUID employerId,
        String employerName,
        UUID providerId,
        String providerName,

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
