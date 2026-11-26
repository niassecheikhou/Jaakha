package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.presentation.ConflictException;

@Repository
public class BookingAdminRepository {
    private final JdbcTemplate jdbcTemplate;

    public BookingAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BookingResponse> findAll(String status) {
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query(bookingQuery(""), (resultSet, rowNumber) -> mapBooking(resultSet));
        }
        return jdbcTemplate.query(bookingQuery(" where b.status = ?"), (resultSet, rowNumber) -> mapBooking(resultSet), status);
    }

    /**
     * Reservations dont l'employeur ou le prestataire a ete cree par cet administrateur.
     */
    public List<BookingResponse> findAllManagedBy(UUID adminId, String status) {
        String managed = " where (employer.created_by = ? or provider_user.created_by = ?)";
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query(bookingQuery(managed), (resultSet, rowNumber) -> mapBooking(resultSet), adminId, adminId);
        }
        return jdbcTemplate.query(bookingQuery(managed + " and b.status = ?"),
                (resultSet, rowNumber) -> mapBooking(resultSet), adminId, adminId, status);
    }

    public boolean isManagedBy(UUID bookingId, UUID adminId) {
        Integer count = jdbcTemplate.queryForObject("""
                        select count(*)
                        from booking b
                        join app_user employer on employer.id = b.employer_id
                        join provider_profile provider on provider.id = b.provider_id
                        left join app_user provider_user on provider_user.id = provider.user_id
                        where b.id = ?
                          and (employer.created_by = ? or provider_user.created_by = ?)
                        """,
                Integer.class,
                bookingId, adminId, adminId
        );
        return count != null && count > 0;
    }

    public Optional<String> findStatus(UUID id) {
        return jdbcTemplate.query(
                "select status from booking where id = ?",
                (resultSet, rowNumber) -> resultSet.getString("status"),
                id
        ).stream().findFirst();
    }

    /**
     * Met a jour le statut seulement s'il n'a pas change depuis la lecture.
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
                left join app_user provider_user on provider_user.id = provider.user_id
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
