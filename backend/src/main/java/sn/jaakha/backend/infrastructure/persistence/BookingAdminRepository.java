package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.BookingResponse;

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

    public BookingResponse updateStatus(UUID id, String status) {
        int updated = jdbcTemplate.update(
                "update booking set status = ?, updated_at = current_timestamp where id = ?",
                status,
                id
        );
        if (updated == 0) {
            throw new IllegalArgumentException("La reservation est introuvable.");
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
