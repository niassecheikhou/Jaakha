package sn.jaakha.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:jaakha;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"app.cors.allowed-origins=http://localhost:5173",
		"spring.mail.host=",
		"app.mail.from=noreply@jaakha.local",
		"app.admin.bootstrap-email="
})
@AutoConfigureMockMvc
class BackendApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void registerReturnsClearValidationErrors() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "cheikh",
								  "email": "dddddd",
								  "phone": "dddddd",
								  "password": "stringst",
								  "role": "EMPLOYEUR"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Donnees invalides."))
				.andExpect(jsonPath("$.fields.email").value("L'adresse email doit etre valide, par exemple cheikh@gmail.com."))
				.andExpect(jsonPath("$.fields.phone").value("Le numero de telephone doit etre un numero senegalais valide au format +221 suivi de 70, 71, 76, 77 ou 78. Exemple: +221771234567."))
				.andExpect(jsonPath("$.fields.password").value("Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractere special."));
	}

}
