package sn.jaakha.backend.application.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import sn.jaakha.backend.domain.model.ProviderAvailability;

public record AvailabilityResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static AvailabilityResponse from(ProviderAvailability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime()
        );
    }
}
