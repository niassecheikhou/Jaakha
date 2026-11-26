package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Reservation creee par un administrateur pour le compte d'un employeur qu'il gere.
 * validate = true : la reservation est acceptee aussitot (statut ACCEPTED).
 */
public record AdminBookingCreateRequest(
        @NotNull(message = "L'employeur est obligatoire.") UUID employerId,

        @NotNull(message = "Le prestataire est obligatoire.") UUID providerId,

        @NotNull(message = "La date de la prestation est obligatoire.")
        @FutureOrPresent(message = "La date de la prestation ne peut pas etre dans le passe.") LocalDate serviceDate,

        @NotNull(message = "L'heure de debut est obligatoire.") LocalTime startTime,

        @Min(value = 1, message = "La duree doit etre d'au moins 1 heure.")
        @Max(value = 12, message = "La duree ne peut pas depasser 12 heures.") int durationHours,

        @NotBlank(message = "L'adresse de la prestation est obligatoire.")
        @Size(max = 255, message = "L'adresse ne doit pas depasser 255 caracteres.") String address,

        @Size(max = 1000, message = "Les instructions ne doivent pas depasser 1000 caracteres.") String instructions,

        boolean validate
) {
}
