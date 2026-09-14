package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReviewCreateRequest(
        @NotNull(message = "La reservation est obligatoire.") UUID bookingId,
        @Min(value = 1, message = "La note minimale est 1.")
        @Max(value = 5, message = "La note maximale est 5.")
        int rating,
        @Size(max = 1000, message = "Le commentaire ne peut pas depasser 1000 caracteres.")
        String comment
) {
}
