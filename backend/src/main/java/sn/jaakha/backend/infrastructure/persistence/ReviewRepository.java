package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.ReviewResponse;

@Repository
public class ReviewRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ReviewResponse create(UUID bookingId, UUID employerId, UUID providerId, int rating, String comment) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        insert into review (
                            id, booking_id, rating, comment, employer_id, provider_id, created_at
                        )
                        values (?, ?, ?, ?, ?, ?, current_timestamp)
                        """,
                id,
                bookingId,
                rating,
                blankToNull(comment),
                employerId,
                providerId
        );
        refreshProviderRating(providerId);
        return findById(id);
    }

    public boolean existsForBooking(UUID bookingId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from review where booking_id = ?",
                Integer.class,
                bookingId
        );
        return count != null && count > 0;
    }

    public ReviewableBooking findReviewableBooking(UUID bookingId, UUID employerId) {
        try {
            return jdbcTemplate.queryForObject("""
                            select id, provider_id
                            from booking
                            where id = ?
                              and employer_id = ?
                              and status = 'DONE'
                            """,
                    (resultSet, rowNumber) -> new ReviewableBooking(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("provider_id", UUID.class)
                    ),
                    bookingId,
                    employerId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("La reservation est introuvable, non terminee ou ne t'appartient pas.");
        }
    }

    public List<ReviewResponse> findByProvider(UUID providerId) {
        return jdbcTemplate.query("""
                        select r.id, r.rating, r.comment, r.employer_id, employer.full_name as employer_name, r.created_at
                        from review r
                        join app_user employer on employer.id = r.employer_id
                        where r.provider_id = ?
                        order by r.created_at desc
                        """,
                (resultSet, rowNumber) -> mapReview(resultSet),
                providerId
        );
    }

    private ReviewResponse findById(UUID id) {
        return jdbcTemplate.queryForObject("""
                        select r.id, r.rating, r.comment, r.employer_id, employer.full_name as employer_name, r.created_at
                        from review r
                        join app_user employer on employer.id = r.employer_id
                        where r.id = ?
                        """,
                (resultSet, rowNumber) -> mapReview(resultSet),
                id
        );
    }

    private void refreshProviderRating(UUID providerId) {
        jdbcTemplate.update("""
                        update provider_profile
                        set average_rating = (
                                select coalesce(avg(rating), 0)
                                from review
                                where provider_id = ?
                            ),
                            review_count = (
                                select count(*)
                                from review
                                where provider_id = ?
                            )
                        where id = ?
                        """,
                providerId,
                providerId,
                providerId
        );
    }

    private ReviewResponse mapReview(ResultSet resultSet) throws SQLException {
        return new ReviewResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getInt("rating"),
                resultSet.getString("comment"),
                resultSet.getObject("employer_id", UUID.class),
                resultSet.getString("employer_name"),
                resultSet.getTimestamp("created_at").toInstant().toString()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ReviewableBooking(UUID id, UUID providerId) {
    }
}
