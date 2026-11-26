package sn.jaakha.backend.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.NotificationResponse;

@Service
public class NotificationService {
    private final JdbcTemplate jdbcTemplate;

    public NotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(UUID userId) {
        return jdbcTemplate.query("""
                        select id, type, title, message, booking_id, recipient_id, read, created_at
                        from notification
                        where recipient_id = ?
                        order by created_at desc
                        limit 30
                        """,
                (resultSet, rowNumber) -> mapNotification(resultSet),
                userId
        );
    }

    @Transactional
    public NotificationResponse markRead(UUID id, UUID userId) {
        int updated = jdbcTemplate.update(
                "update notification set read = true where id = ? and recipient_id = ?",
                id,
                userId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("La notification est introuvable.");
        }
        return jdbcTemplate.queryForObject("""
                        select id, type, title, message, booking_id, recipient_id, read, created_at
                        from notification
                        where id = ? and recipient_id = ?
                        """,
                (resultSet, rowNumber) -> mapNotification(resultSet),
                id,
                userId
        );
    }

    @Transactional
    public void notifyAdminsAboutBooking(BookingResponse booking) {
        List<UUID> adminIds = jdbcTemplate.query("""
                        select id
                        from app_user
                        where role in ('ADMIN', 'SUPER_ADMIN')
                          and active = true
                          and archived = false
                        """,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class)
        );
        for (UUID adminId : adminIds) {
            jdbcTemplate.update("""
                            insert into notification (
                                id, type, title, message, booking_id, recipient_id, read, created_at
                            )
                            values (?, 'BOOKING_REQUESTED', 'Nouvelle reservation',
                                    ?, ?, ?, false, current_timestamp)
                            """,
                    UUID.randomUUID(),
                    "Nouvelle demande de reservation de " + booking.employerName()
                            + " pour " + booking.providerName() + ".",
                    booking.id(),
                    adminId
            );
        }
    }

    @Transactional
    public void notifyProviderAboutBookingRequested(BookingResponse booking) {
        providerUserId(booking.providerId()).forEach(providerUserId -> insertNotification(
                "BOOKING_REQUESTED",
                "Nouvelle demande",
                "Nouvelle demande de reservation de " + booking.employerName() + ".",
                booking.id(),
                providerUserId
        ));
    }

    @Transactional
    public void notifyParticipantsAboutBookingStatus(BookingResponse booking) {
        if (!"ACCEPTED".equals(booking.status())
                && !"IN_PROGRESS".equals(booking.status())
                && !"DONE".equals(booking.status())) {
            return;
        }

        String title = switch (booking.status()) {
            case "ACCEPTED" -> "Reservation acceptee";
            case "IN_PROGRESS" -> "Reservation en cours";
            case "DONE" -> "Reservation terminee";
            default -> "Reservation mise a jour";
        };
        String employerMessage = switch (booking.status()) {
            case "ACCEPTED" -> "Votre demande avec " + booking.providerName() + " a ete acceptee.";
            case "IN_PROGRESS" -> "Votre reservation avec " + booking.providerName() + " est en cours.";
            case "DONE" -> "Votre reservation avec " + booking.providerName() + " est terminee.";
            default -> "Votre reservation avec " + booking.providerName() + " a ete mise a jour.";
        };
        String providerMessage = switch (booking.status()) {
            case "ACCEPTED" -> "La demande de " + booking.employerName() + " a ete acceptee par l'administration.";
            case "IN_PROGRESS" -> "La reservation de " + booking.employerName() + " est en cours.";
            case "DONE" -> "La reservation de " + booking.employerName() + " est terminee.";
            default -> "La reservation de " + booking.employerName() + " a ete mise a jour.";
        };

        insertNotification("BOOKING_" + booking.status(), title, employerMessage, booking.id(), booking.employerId());
        providerUserId(booking.providerId()).forEach(providerUserId -> insertNotification(
                "BOOKING_" + booking.status(),
                title,
                providerMessage,
                booking.id(),
                providerUserId
        ));
    }

    private List<UUID> providerUserId(UUID providerProfileId) {
        return jdbcTemplate.query("""
                        select user_id
                        from provider_profile
                        where id = ?
                        """,
                (resultSet, rowNumber) -> resultSet.getObject("user_id", UUID.class),
                providerProfileId
        );
    }

    private void insertNotification(String type, String title, String message, UUID bookingId, UUID recipientId) {
        jdbcTemplate.update("""
                        insert into notification (
                            id, type, title, message, booking_id, recipient_id, read, created_at
                        )
                        values (?, ?, ?, ?, ?, ?, false, current_timestamp)
                        """,
                UUID.randomUUID(),
                type,
                title,
                message,
                bookingId,
                recipientId
        );
    }

    private NotificationResponse mapNotification(ResultSet resultSet) throws SQLException {
        return new NotificationResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("type"),
                resultSet.getString("title"),
                resultSet.getString("message"),
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getObject("recipient_id", UUID.class),
                resultSet.getBoolean("read"),
                resultSet.getTimestamp("created_at").toInstant().toString()
        );
    }
}
