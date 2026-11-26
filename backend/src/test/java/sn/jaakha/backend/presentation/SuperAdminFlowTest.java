package sn.jaakha.backend.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Scenario HTTP complet (H2) : le super administrateur cree un administrateur, qui cree a son tour
 * un employeur mais ne peut ni creer d'administrateur ni voir les comptes d'administration.
 * Desactive par le super administrateur, l'administrateur perd aussitot ses droits.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SuperAdminFlowTest {
    private static final String PASSWORD = "Secret1!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from account_profile where owner_key in (select cast(id as varchar) from app_user where email like 'flow-%@jaankha.test')");
        jdbcTemplate.update("delete from session_token where user_id in (select id from app_user where email like 'flow-%@jaankha.test')");
        jdbcTemplate.update("delete from password_reset_token where user_id in (select id from app_user where email like 'flow-%@jaankha.test')");
        jdbcTemplate.update("delete from audit_log where actor_id in (select id from app_user where email like 'flow-%@jaankha.test')");
        jdbcTemplate.update("delete from app_user where email like 'flow-%@jaankha.test'");
    }

    @Test
    void superAdminValidatesAdminDossierThenAdminManagesOnlyEmployersAndProviders() throws Exception {
        jdbcTemplate.update("""
                        insert into app_user (id, full_name, email, phone, password_hash, role,
                                              email_verified, active, archived, created_at, updated_at)
                        values (?, 'Super Admin Flow', 'flow-super@jaankha.test', '+221770000010', ?, 'SUPER_ADMIN',
                                true, true, false, current_timestamp, current_timestamp)
                        """,
                UUID.randomUUID(), passwordEncoder.encode(PASSWORD));
        String superToken = login("flow-super@jaankha.test");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        String adminJson = mockMvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Admin Secondaire", "flow-admin@jaankha.test", "+221770000011", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(false))
                .andReturn().getResponse().getContentAsString();
        String adminId = JsonPath.read(adminJson, "$.id");
        String adminToken = login("flow-admin@jaankha.test");

        // Compte en attente : pas encore d'acces a l'administration, mais il complete son dossier.
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/account/me").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zone\":\"Dakar Plateau\",\"address\":\"Rue 10, Medina\",\"experienceYears\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Rue 10, Medina"));

        // Sans piece d'identite, le super administrateur ne peut pas valider le compte.
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/active").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("recto de la piece d'identite")));

        // Pieces deposees (simulees en base : l'upload d'images est couvert par ses propres tests).
        jdbcTemplate.update(
                "update account_profile set identity_photo_url = 'recto.jpg', identity_photo_back_url = 'verso.jpg' where owner_key = ?",
                adminId);
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/active").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.address").value("Rue 10, Medina"));

        // L'administrateur cree un employeur, mais pas un autre administrateur.
        mockMvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Employeur Flow", "flow-employer@jaankha.test", "+221770000012", "EMPLOYEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEUR"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Employeur Inscrit", "flow-self@jaankha.test", "+221770000014", "EMPLOYEUR")))
                .andExpect(status().isCreated());
        String selfId = jdbcTemplate.queryForObject("select cast(id as varchar) from app_user where email = 'flow-self@jaankha.test'", String.class);
        mockMvc.perform(patch("/api/admin/users/" + selfId + "/archive").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"archived\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users/" + selfId + "/identity-photo").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Admin Pirate", "flow-other@jaankha.test", "+221770000013", "ADMIN")))
                .andExpect(status().isForbidden());

        // L'historique est reserve au super administrateur.
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + superToken)
                        .param("actorId", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action").value(org.hamcrest.Matchers.hasItems("CONNEXION", "PROFIL_MIS_A_JOUR", "COMPTE_CREE")))
                .andExpect(jsonPath("$[?(@.action == 'COMPTE_CREE')].targetLabel").value(org.hamcrest.Matchers.contains("Employeur Flow")))
                .andExpect(jsonPath("$[*].actorName").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Admin Secondaire"))));
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + superToken)
                        .param("action", "COMPTE_VALIDE").param("actorRole", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.targetId == '" + adminId + "')].actorName").value(org.hamcrest.Matchers.contains("Super Admin Flow")));
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + superToken)
                        .param("from", "2999-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + superToken)
                        .param("action", "INCONNUE"))
                .andExpect(status().isBadRequest());

        // Il ne voit pas les comptes d'administration et ne peut pas les modifier.
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.role == 'ADMIN' || @.role == 'SUPER_ADMIN')]").isEmpty())
                // Seuls les comptes qu'il a crees : pas l'employeur inscrit librement.
                .andExpect(jsonPath("$[*].email").value(org.hamcrest.Matchers.contains("flow-employer@jaankha.test")));
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + superToken))
                .andExpect(jsonPath("$[*].email").value(org.hamcrest.Matchers.hasItem("flow-self@jaankha.test")));

        // Les messages de contact sont reserves au super administrateur.
        mockMvc.perform(get("/api/admin/contact-messages").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/contact-messages").header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/archive").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"archived\":true}"))
                .andExpect(status().isForbidden());

        // Le super administrateur voit l'administrateur et le desactive : ses droits tombent aussitot.
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + superToken))
                .andExpect(jsonPath("$[?(@.email == 'flow-admin@jaankha.test')].role").value(org.hamcrest.Matchers.contains("ADMIN")));
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/active").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String createBody(String fullName, String email, String phone, String role) {
        return "{\"fullName\":\"" + fullName + "\",\"email\":\"" + email + "\",\"phone\":\"" + phone
                + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"" + role + "\"}";
    }
}
