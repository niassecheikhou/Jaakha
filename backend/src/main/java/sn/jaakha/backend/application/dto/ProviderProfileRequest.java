package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ProviderProfileRequest(
        String bio,
        @NotBlank(message = "La zone est obligatoire.") String zone,
        @NotNull(message = "Le tarif mensuel est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le tarif mensuel doit etre superieur ou egal a 0.") BigDecimal monthlyRate,
        @Min(value = 0, message = "L'experience doit etre superieure ou egale a 0.") int experienceYears,
        List<String> skills,
        List<AvailabilityResponse> availability
) {
}
