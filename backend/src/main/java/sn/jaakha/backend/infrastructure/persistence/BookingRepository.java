package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.presentation.ConflictException;

@Repository
public class BookingRepository {
    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BookingResponse create(UUID employerId, UUID providerId) {
        UUID id = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now();
        LocalTime startTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        jdbcTemplate.update("""
                        insert into booking (
                            id, status, service_date, start_time, duration_hours, address,
                            instructions, employer_id, provider_id, created_at, updated_at
                        )
                        values (?, 'REQUESTED', ?, ?, 1, 'Adresse a confirmer',
                                'Demande creee depuis le bouton Reserver.', ?, ?, current_timestamp, current_timestamp)
                        """,
                id,
                serviceDate,
                startTime,
                employerId,
                providerId
        );
        return findById(id);
    }

    /** Reservation creee avec ses details (par un administrateur pour un employeur). */
    public BookingResponse create(
            UUID employerId,
            UUID providerId,
            LocalDate serviceDate,
            LocalTime startTime,
            int durationHours,
            String address,
            String instructions
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        insert into booking (
                            id, status, service_date, start_time, duration_hours, address,
                            instructions, employer_id, provider_id, created_at, updated_at
                        )
                        values (?, 'REQUESTED', ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                        """,
                id, serviceDate, startTime.truncatedTo(ChronoUnit.MINUTES), durationHours, address, instructions,
                employerId, providerId
        );
        return findById(id);
    }

    public List<BookingResponse> findMine(UUID userId, String role) {
        if ("PRESTATAIRE".equals(role)) {
            return jdbcTemplate.query(
                    bookingQuery(" where provider.user_id = ?"),
                    (resultSet, rowNumber) -> mapBooking(resultSet),
                    userId
            );
        }
        return jdbcTemplate.query(
                bookingQuery(" where b.employer_id = ?"),
                (resultSet, rowNumber) -> mapBooking(resultSet),
                userId
        );
    }

    /**
     * Statut courant d'une reservation, uniquement si elle appartient a l'acteur (employeur ou prestataire).
     */
    public Optional<String> findStatusOwnedBy(UUID id, UUID actorId, String actorRole) {
        String ownershipClause = "PRESTATAIRE".equals(actorRole)
                ? " and provider_id in (select id from provider_profile where user_id = ?)"
                : " and employer_id = ?";
        return jdbcTemplate.query(
                "select status from booking where id = ?" + ownershipClause,
                (resultSet, rowNumber) -> resultSet.getString("status"),
                id,
                actorId
        ).stream().findFirst();
    }

    /**
     * Met a jour le statut seulement s'il n'a pas change depuis la lecture (evite deux transitions concurrentes).
     */
    public BookingResponse updateStatus(UUID id, String expectedStatus, String newStatus) {
        int updated = jdbcTemplate.update(
                "update booking set status = ?, updated_at = current_timestamp where id = ? and status = ?",
                newStatus,
                id,
                expectedStatus
        );
        if (updated == 0) {
            throw new ConflictException("La reservation a ete modifiee entre-temps. Recharge la page puis reessaie.");
        }
        return findById(id);
    }

    public BookingResponse findById(UUID id) {
        return jdbcTemplate.queryForObject(
                bookingQuery(" where b.id = ?"),
                (resultSet, rowNumber) -> mapBooking(resultSet),
                id
        );
    }

    private String bookingQuery(String whereClause) {
        return """
                select b.id, b.status, b.service_date, b.start_time, b.duration_hours, b.address,
                       b.instructions, b.employer_id, employer.full_name as employer_name,
                       b.provider_id, provider.full_name as provider_name, b.created_at
                from booking b
                join app_user employer on employer.id = b.employer_id
                join provider_profile provider on provider.id = b.provider_id
                """ + whereClause + " order by b.created_at desc";
    }

    private BookingResponse mapBooking(ResultSet resultSet) throws SQLException {
        return new BookingResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("status"),
                resultSet.getDate("service_date").toLocalDate(),
                resultSet.getTime("start_time").toLocalTime(),
                resultSet.getInt("duration_hours"),
                resultSet.getString("address"),
                resultSet.getString("instructions"),
                resultSet.getObject("employer_id", UUID.class),
                resultSet.getString("employer_name"),
                resultSet.getObject("provider_id", UUID.class),
                resultSet.getString("provider_name"),
                resultSet.getTimestamp("created_at").toInstant().toString()
        );
    }
}
