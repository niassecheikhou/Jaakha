package sn.jaakha.backend.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import sn.jaakha.backend.application.dto.UserResponse;

@Repository
public class UserAccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserResponse> findAll() {
        return jdbcTemplate.query("""
                        select u.id, u.full_name, u.email, u.phone, u.role, u.email_verified,
                               u.active, u.archived, u.created_at, p.photo_url, p.identity_photo_url, p.identity_photo_back_url,
                               p.bio, p.zone, p.monthly_rate, p.experience_years,
                               p.skills, p.guardian_full_name, p.guardian_phone
                        from app_user u
                        left join account_profile p on p.owner_key = cast(u.id as varchar)
                        order by u.created_at desc
                        """,
                (resultSet, rowNumber) -> mapUser(resultSet)
        );
    }

    public Optional<UserResponse> findById(UUID id) {
        List<UserResponse> users = jdbcTemplate.query("""
                        select u.id, u.full_name, u.email, u.phone, u.role, u.email_verified,
                               u.active, u.archived, u.created_at, p.photo_url, p.identity_photo_url, p.identity_photo_back_url,
                               p.bio, p.zone, p.monthly_rate, p.experience_years,
                               p.skills, p.guardian_full_name, p.guardian_phone
                        from app_user u
                        left join account_profile p on p.owner_key = cast(u.id as varchar)
                        where u.id = ?
                        """,
                (resultSet, rowNumber) -> mapUser(resultSet),
                id
        );
        return users.stream().findFirst();
    }

    public void create(
            UUID id,
            String fullName,
            String email,
            String phone,
            String passwordHash,
            String role,
            boolean active
    ) {
        create(id, fullName, email, phone, passwordHash, role, active, null);
    }

    /** Cree un compte en memorisant l'administrateur qui l'a cree (null pour une inscription). */
    public void create(
            UUID id,
            String fullName,
            String email,
            String phone,
            String passwordHash,
            String role,
            boolean active,
            UUID createdBy
    ) {
        jdbcTemplate.update("""
                        insert into app_user (
                            id, full_name, email, phone, password_hash, role,
                            email_verified, active, archived, created_by, created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, false, ?, false, ?, current_timestamp, current_timestamp)
                        """,
                id,
                fullName,
                email,
                phone,
                passwordHash,
                role,
                active,
                createdBy
        );
    }

    public Optional<UUID> findCreatedBy(UUID id) {
        return jdbcTemplate.query(
                "select created_by from app_user where id = ?",
                (resultSet, rowNumber) -> Optional.ofNullable(resultSet.getObject("created_by", UUID.class)),
                id
        ).stream().findFirst().flatMap(value -> value);
    }

    public void updateActive(UUID id, boolean active) {
        jdbcTemplate.update(
                "update app_user set active = ?, updated_at = current_timestamp where id = ?",
                active,
                id
        );
    }

    public void updateArchived(UUID id, boolean archived) {
        jdbcTemplate.update(
                "update app_user set archived = ?, active = case when ? then false else active end, updated_at = current_timestamp where id = ?",
                archived,
                archived,
                id
        );
    }

    public Set<UUID> visibleProviderUserIds() {
        return new LinkedHashSet<>(jdbcTemplate.query("""
                        select id
                        from app_user
                        where role = 'PRESTATAIRE'
                          and active = true
                          and archived = false
                        """,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class)
        ));
    }

    private UserResponse mapUser(ResultSet resultSet) throws SQLException {
        return new UserResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("role"),
                resultSet.getBoolean("email_verified"),
                resultSet.getBoolean("active"),
                resultSet.getBoolean("archived"),
                resultSet.getString("photo_url"),
                adminIdentityPhotoUrl(resultSet, "identity_photo_url", ""),
                adminIdentityPhotoUrl(resultSet, "identity_photo_back_url", "/back"),
                resultSet.getString("bio"),
                resultSet.getString("zone"),
                resultSet.getBigDecimal("monthly_rate"),
                resultSet.getInt("experience_years"),
                splitSkills(resultSet.getString("skills")),
                resultSet.getString("guardian_full_name"),
                resultSet.getString("guardian_phone"),
                createdAt(resultSet)
        );
    }

    /**
     * URL protegee (reservee a l'admin) d'une face de la piece d'identite, ou null si elle n'est pas deposee.
     */
    private String adminIdentityPhotoUrl(ResultSet resultSet, String column, String sidePath) throws SQLException {
        if (resultSet.getString(column) == null) {
            return null;
        }
        return "/api/admin/users/" + resultSet.getObject("id", UUID.class) + "/identity-photo" + sidePath;
    }

    private String createdAt(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return createdAt == null ? null : createdAt.toInstant().toString();
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
