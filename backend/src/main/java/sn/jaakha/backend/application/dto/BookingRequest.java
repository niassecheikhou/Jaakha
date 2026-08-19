package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingRequest(
        @NotNull UUID providerId,
        @NotNull @FutureOrPresent LocalDate serviceDate,
        @NotNull LocalTime startTime,
        @Min(1) @Max(24) int durationHours,
        @NotBlank String address,
        String instructions
) {
}
