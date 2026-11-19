package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

public record AccountProfileRequest(

        String bio,

        String zone,

        @DecimalMin(value = "0.0", message = "Le tarif mensuel ne peut pas etre negatif.")
        BigDecimal monthlyRate,

        @Min(value = 0, message = "L'experience doit etre superieure ou egale a 0.")
        int experienceYears,

        List<String> skills,

        String guardianFullName,

        @Pattern(
                regexp = "^$|^\\+221(70|71|76|77|78)[0-9]{7}$",
                message = "Le telephone du tuteur doit etre senegalais: +221 suivi de 70, 71, 76, 77 ou 78 puis 7 chiffres."
        ) String guardianPhone
) {
}
