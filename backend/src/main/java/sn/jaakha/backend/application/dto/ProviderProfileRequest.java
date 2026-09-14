package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ProviderProfileRequest(
        String photoUrl,
        String bio,

        @NotBlank String zone,
        @NotNull @DecimalMin("0.0") BigDecimal hourlyRate,
        @Min(0) int experienceYears,
        List<String> skills,
        List<AvailabilityResponse>
) {
}
