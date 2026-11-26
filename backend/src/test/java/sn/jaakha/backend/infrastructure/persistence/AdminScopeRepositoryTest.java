package sn.jaakha.backend.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import sn.jaakha.backend.application.NotificationService;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;

/**
 * Test d'integration (H2) du perimetre d'un administrateur : il ne voit que les reservations
 * impliquant un compte qu'il a cree, et seul lui (avec les super admins) en est notifie.
 */
@SpringBootTest
class AdminScopeRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BookingAdminRepository bookingAdminRepository;
    @Autowired
    private ProviderProfileRepository providerProfileRepository;
    @Autowired
    private NotificationService notificationService;

    private final UUID adminA = UUID.randomUUID();
    private final UUID adminB = UUID.randomUUID();
    private final UUID superAdmin = UUID.randomUUID();
    private final UUID employer = UUID.randomUUID();
    private final UUID providerUser = UUID.randomUUID();
    private UUID providerProfileId;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from notification where recipient_id in (?, ?, ?)", adminA, adminB, superAdmin);
        jdbcTemplate.update("delete from booking where employer_id = ?", employer);
        if (providerProfileId != null) {
            providerProfileRepository.deleteById(providerProfileId);
        }
        jdbcTemplate.update("delete from app_user where email like 'scope-%@jaankha.test'");
    }

    @Test
    void adminSeesAndIsNotifiedOnlyAboutBookingsOfHisAccounts() {
        insertUser(superAdmin, "scope-super", "+221770000020", "SUPER_ADMIN", null);
        insertUser(adminA, "scope-admin-a", "+221770000021", "ADMIN", superAdmin);
        insertUser(adminB, "scope-admin-b", "+221770000022", "ADMIN", superAdmin);
        insertUser(employer, "scope-employer", "+221770000023", "EMPLOYEUR", adminA);
        insertUser(providerUser, "scope-provider", "+221770000024", "PRESTATAIRE", null);
        providerProfileId = providerProfileRepository.save(
                new ProviderProfile(providerUser.toString(), providerUser, "Prestataire Scope")).getId();
        UUID bookingId = UUID.randomUUID();
        jdbcTemplate.update("""
                        insert into booking (id, status, service_date, start_time, duration_hours, address,
                                             employer_id, provider_id, created_at, updated_at)
                        values (?, 'REQUESTED', current_date, current_time, 2, 'Dakar', ?, ?, current_timestamp, current_timestamp)
                        """,
                bookingId, employer, providerProfileId);

        assertThat(bookingAdminRepository.findAllManagedBy(adminA, null)).extracting(BookingResponse::id).containsExactly(bookingId);
        assertThat(bookingAdminRepository.findAllManagedBy(adminA, "DONE")).isEmpty();
        assertThat(bookingAdminRepository.findAllManagedBy(adminB, null)).isEmpty();
        assertThat(bookingAdminRepository.isManagedBy(bookingId, adminA)).isTrue();
        assertThat(bookingAdminRepository.isManagedBy(bookingId, adminB)).isFalse();

        BookingResponse booking = bookingAdminRepository.findAll(null).stream()
                .filter(candidate -> candidate.id().equals(bookingId))
                .findFirst()
                .orElseThrow();
        notificationService.notifyAdminsAboutBooking(booking);

        assertThat(notificationCount(adminA)).isEqualTo(1);
        assertThat(notificationCount(superAdmin)).isEqualTo(1);
        assertThat(notificationCount(adminB)).isZero();
    }

    private void insertUser(UUID id, String emailPrefix, String phone, String role, UUID createdBy) {
        jdbcTemplate.update("""
                        insert into app_user (id, full_name, email, phone, password_hash, role,
                                              email_verified, active, archived, created_by, created_at, updated_at)
                        values (?, ?, ?, ?, 'hash', ?, true, true, false, ?, current_timestamp, current_timestamp)
                        """,
                id, "Compte " + emailPrefix, emailPrefix + "@jaankha.test", phone, role, createdBy);
    }

    private int notificationCount(UUID recipientId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from notification where recipient_id = ?", Integer.class, recipientId);
        return count == null ? 0 : count;
    }
}
