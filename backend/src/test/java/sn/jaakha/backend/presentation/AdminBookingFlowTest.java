package sn.jaakha.backend.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
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
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;

/**
 * Scenario HTTP (H2) : un administrateur reserve pour un employeur qu'il a cree et valide la
 * reservation d'emblee ; un autre administrateur ne peut pas reserver pour cet employeur.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminBookingFlowTest {
    private static final String PASSWORD = "Secret1!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProviderProfileRepository providerProfileRepository;

    private final UUID adminA = UUID.randomUUID();
    private final UUID adminB = UUID.randomUUID();
    private final UUID employer = UUID.randomUUID();
    private final UUID providerUser = UUID.randomUUID();
    private UUID providerProfileId;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from notification where recipient_id in (?, ?, ?, ?)", adminA, adminB, employer, providerUser);
        jdbcTemplate.update("delete from booking where employer_id = ?", employer);
        jdbcTemplate.update("delete from audit_log where actor_id in (?, ?, ?)", adminA, adminB, employer);
        jdbcTemplate.update("delete from session_token where user_id in (?, ?, ?)", adminA, adminB, employer);
        if (providerProfileId != null) {
            providerProfileRepository.deleteById(providerProfileId);
        }
        jdbcTemplate.update("delete from app_user where email like 'abook-%@jaankha.test'");
    }

    @Test
    void adminBooksForHisEmployerAndValidatesImmediately() throws Exception {
        insertUser(adminA, "abook-admin-a", "+221770000030", "ADMIN", null);
        insertUser(adminB, "abook-admin-b", "+221770000031", "ADMIN", null);
        insertUser(employer, "abook-employer", "+221770000032", "EMPLOYEUR", adminA);
        insertUser(providerUser, "abook-provider", "+221770000033", "PRESTATAIRE", null);
        ProviderProfile profile = new ProviderProfile(providerUser.toString(), providerUser, "Prestataire Abook");
        profile.setStatus(ProviderStatus.APPROVED);
        providerProfileId = providerProfileRepository.save(profile).getId();
        String tokenA = login("abook-admin-a");
        String tokenB = login("abook-admin-b");
        String tomorrow = LocalDate.now().plusDays(1).toString();

        String json = mockMvc.perform(post("/api/admin/bookings").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(tomorrow, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.employerName").value("Compte abook-employer"))
                .andExpect(jsonPath("$.address").value("Rue 10, Medina"))
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(json, "$.id");

        // La reservation apparait chez l'employeur et dans la liste de l'administrateur.
        String employerToken = login("abook-employer");
        mockMvc.perform(get("/api/bookings/me").header("Authorization", "Bearer " + employerToken))
                .andExpect(jsonPath("$[?(@.id == '" + bookingId + "')].status").value(org.hamcrest.Matchers.contains("ACCEPTED")));
        mockMvc.perform(get("/api/admin/bookings").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.hasItem(bookingId)));

        // Un autre administrateur ne gere pas cet employeur ; une date passee est refusee.
        mockMvc.perform(post("/api/admin/bookings").header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(tomorrow, false)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/bookings").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusDays(1).toString(), false)))
                .andExpect(status().isBadRequest());
    }

    private String body(String date, boolean validate) {
        return "{\"employerId\":\"" + employer + "\",\"providerId\":\"" + providerProfileId + "\",\"serviceDate\":\"" + date
                + "\",\"startTime\":\"09:00\",\"durationHours\":3,\"address\":\"Rue 10, Medina\","
                + "\"instructions\":\"Menage complet\",\"validate\":" + validate + "}";
    }

    private void insertUser(UUID id, String emailPrefix, String phone, String role, UUID createdBy) {
        jdbcTemplate.update("""
                        insert into app_user (id, full_name, email, phone, password_hash, role,
                                              email_verified, active, archived, created_by, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, true, true, false, ?, current_timestamp, current_timestamp)
                        """,
                id, "Compte " + emailPrefix, emailPrefix + "@jaankha.test", phone, passwordEncoder.encode(PASSWORD), role, createdBy);
    }

    private String login(String emailPrefix) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + emailPrefix + "@jaankha.test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
