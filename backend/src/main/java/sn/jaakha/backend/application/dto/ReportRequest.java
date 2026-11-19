package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ReportRequest(
        UUID bookingId,
        @NotBlank String reason,
        String details
) {
}
