package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(min = 5, message = "Le nom complet doit contenir au moins 5 caracteres.") String fullName,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "L'email doit etre valide, par exemple nom@gmail.com.") String email,

        @NotBlank(message = "Le numero de telephone est obligatoire.")
        @Pattern(
                regexp = "^\\+221(70|71|76|77|78)[0-9]{7}$",
                message = "Le telephone doit etre senegalais: +221 suivi de 70, 71, 76, 77 ou 78 puis 7 chiffres."
        ) String phone,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$",
                message = "Le mot de passe doit contenir au moins 6 caracteres, une majuscule, une minuscule, un chiffre et un caractere special."
        ) String password,

        @Pattern(regexp = "EMPLOYEUR|PRESTATAIRE", message = "Le role doit etre EMPLOYEUR ou PRESTATAIRE.") String role
) {
}
