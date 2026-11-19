package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.ContactMessageResponse;

@Repository
public class ContactMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public ContactMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ContactMessageResponse create(
            UUID id,
            String name,
            String email,
            String phone,
            String company,
            String message
    ) {
        jdbcTemplate.update(
                """
                        insert into contact_message (
                            id, name, email, phone, company, message, created_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                name,
                email,
                phone,
                company,
                message,
                LocalDateTime.now()
        );

        return findById(id);
    }

    public List<ContactMessageResponse> findAll() {
        return jdbcTemplate.query(
                """
                        select id, name, email, phone, company, message, created_at
                        from contact_message
                        order by created_at desc
                        """,
                this::mapRow
        );
    }

    private ContactMessageResponse findById(UUID id) {
        return jdbcTemplate.queryForObject(
                """
                        select id, name, email, phone, company, message, created_at
                        from contact_message
                        where id = ?
                        """,
                this::mapRow,
                id
        );
    }

    private ContactMessageResponse mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ContactMessageResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("company"),
                resultSet.getString("message"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
