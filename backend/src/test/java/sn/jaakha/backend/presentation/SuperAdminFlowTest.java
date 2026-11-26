package sn.jaakha.backend.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        jdbcTemplate.update("delete from session_token where user_id in (select id from app_user where email like 'flow-%@jaankha.test')");
        jdbcTemplate.update("delete from password_reset_token where user_id in (select id from app_user where email like 'flow-%@jaankha.test')");
        jdbcTemplate.update("delete from app_user where email like 'flow-%@jaankha.test'");
    }

    @Test
    void superAdminCreatesAdminWhoManagesOnlyEmployersAndProviders() throws Exception {
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
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        String adminId = JsonPath.read(adminJson, "$.id");
        String adminToken = login("flow-admin@jaankha.test");

        // L'administrateur cree un employeur, mais pas un autre administrateur.
        mockMvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Employeur Flow", "flow-employer@jaankha.test", "+221770000012", "EMPLOYEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEUR"));
        mockMvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Admin Pirate", "flow-other@jaankha.test", "+221770000013", "ADMIN")))
                .andExpect(status().isForbidden());

        // Il ne voit pas les comptes d'administration et ne peut pas les modifier.
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.role == 'ADMIN' || @.role == 'SUPER_ADMIN')]").isEmpty());
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
