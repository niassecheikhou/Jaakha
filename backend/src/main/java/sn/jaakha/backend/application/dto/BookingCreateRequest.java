package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BookingCreateRequest(
        @NotNull(message = "Le prestataire est obligatoire pour creer une reservation.")
        UUID providerId
) {
}
