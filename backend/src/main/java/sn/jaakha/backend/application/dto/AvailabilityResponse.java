package sn.jaakha.backend.application.dto;

public record AvailabilityResponse(
        String dayOfWeek,
        String startTime,
        String endTime
) {
}
