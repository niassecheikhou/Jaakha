package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.AuditLogResponse;

/**
 * Journal des actions (table audit_log, creee par UserAccountSchemaInitializer).
 * Le nom et le role de l'auteur sont recopies : l'historique reste lisible meme si le compte change.
 */
@Repository
public class AuditLogRepository {
    /** Nombre maximal d'entrees renvoyees par une recherche. */
    public static final int MAX_RESULTS = 1000;

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            UUID actorId,
            String actorName,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String targetLabel,
            String details
    ) {
        jdbcTemplate.update("""
                        insert into audit_log (
                            id, actor_id, actor_name, actor_role, action,
                            target_type, target_id, target_label, details, created_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                        """,
                UUID.randomUUID(), actorId, actorName, actorRole, action,
                targetType, targetId, targetLabel, details
        );
    }

    /** Recherche filtree (chaque filtre est optionnel), de la plus recente a la plus ancienne. */
    public List<AuditLogResponse> search(Instant from, Instant to, UUID actorId, String actorRole, String action) {
        StringBuilder sql = new StringBuilder("""
                select id, actor_id, actor_name, actor_role, action,
                       target_type, target_id, target_label, details, created_at
                from audit_log
                where 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (from != null) {
            sql.append(" and created_at >= ?");
            parameters.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" and created_at < ?");
            parameters.add(Timestamp.from(to));
        }
        if (actorId != null) {
            sql.append(" and actor_id = ?");
            parameters.add(actorId);
        }
        if (actorRole != null) {
            sql.append(" and actor_role = ?");
            parameters.add(actorRole);
        }
        if (action != null) {
            sql.append(" and action = ?");
            parameters.add(action);
        }
        sql.append(" order by created_at desc limit ").append(MAX_RESULTS);
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> map(resultSet), parameters.toArray());
    }

    private AuditLogResponse map(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new AuditLogResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("actor_id", UUID.class),
                resultSet.getString("actor_name"),
                resultSet.getString("actor_role"),
                resultSet.getString("action"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("target_label"),
                resultSet.getString("details"),
                createdAt == null ? null : createdAt.toInstant().toString()
        );
    }
}
