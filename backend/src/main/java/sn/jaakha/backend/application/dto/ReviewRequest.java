package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record ReviewRequest(
        @Min(1) @Max(5) int rating,
        String comment,
        UUID bookingId
) {
}
