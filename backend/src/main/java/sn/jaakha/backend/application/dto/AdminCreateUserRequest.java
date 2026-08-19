package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import sn.jaakha.backend.domain.enums.Role;

public record AdminCreateUserRequest(
        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(min = 5, message = "Le nom complet doit contenir au moins 5 caracteres.")
        String fullName,

        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email doit etre valide, par exemple cheikh@gmail.com.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "L'adresse email doit etre valide, par exemple cheikh@gmail.com."
        )
        String email,

        @NotBlank(message = "Le numero de telephone est obligatoire.")
        @Pattern(
                regexp = "^\\+221(70|71|76|77|78)[0-9]{7}$",
                message = "Le numero de telephone doit etre un numero senegalais valide au format +221 suivi de 70, 71, 76, 77 ou 78. Exemple: +221771234567."
        )
        String phone,

        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$",
                message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractere special."
        )
        String password,

        @NotNull(message = "Le role est obligatoire: EMPLOYEUR, PRESTATAIRE ou ADMIN.")
        Role role
) {
}
