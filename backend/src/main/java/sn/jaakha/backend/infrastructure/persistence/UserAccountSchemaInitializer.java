package sn.jaakha.backend.infrastructure.persistence;

import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserAccountSchemaInitializer {
    @Bean
    ApplicationRunner ensureUserAccountTables(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PHONE:+221770000000}") String adminPhone,
            @Value("${ADMIN_PASSWORD:ChangeMe123!}") String adminPassword
    ) {
        return args -> {
            jdbcTemplate.execute("""
                    create table if not exists app_user (
                        id uuid primary key,
                        full_name varchar(160) not null,
                        email varchar(255) not null unique,
                        phone varchar(40) not null unique,
                        password_hash varchar(255) not null,
                        role varchar(40) not null,
                        email_verified boolean not null default false,
                        active boolean not null default false,
                        archived boolean not null default false,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            jdbcTemplate.execute("""
                    create table if not exists session_token (
                        id uuid primary key,
                        token varchar(255) not null unique,
                        user_id uuid not null,
                        expires_at timestamp not null,
                        created_at timestamp not null
                    )
                    """);
            jdbcTemplate.execute(
                    "alter table app_user add column if not exists archived boolean not null default false"
            );
            jdbcTemplate.execute("""
                    create table if not exists booking (
                        id uuid primary key,
                        status varchar(40) not null,
                        service_date date not null,
                        start_time time not null,
                        duration_hours integer not null,
                        address varchar(255) not null,
                        instructions varchar(1000),
                        employer_id uuid not null,
                        provider_id uuid not null,
                        created_at timestamp not null,
                        updated_at timestamp not null
                    )
                    """);
            jdbcTemplate.execute("alter table booking add column if not exists status varchar(40) not null default 'REQUESTED'");
            jdbcTemplate.execute("alter table booking add column if not exists service_date date not null default current_date");
            jdbcTemplate.execute("alter table booking add column if not exists start_time time not null default current_time");
            jdbcTemplate.execute("alter table booking add column if not exists duration_hours integer not null default 1");
            jdbcTemplate.execute("alter table booking add column if not exists address varchar(255) not null default 'Adresse a confirmer'");
            jdbcTemplate.execute("alter table booking add column if not exists instructions varchar(1000)");
            jdbcTemplate.execute("alter table booking add column if not exists employer_id uuid");
            jdbcTemplate.execute("alter table booking add column if not exists provider_id uuid");
            jdbcTemplate.execute("alter table booking add column if not exists created_at timestamp not null default current_timestamp");
            jdbcTemplate.execute("alter table booking add column if not exists updated_at timestamp not null default current_timestamp");
            jdbcTemplate.execute("""
                    create table if not exists review (
                        id uuid primary key,
                        booking_id uuid not null unique,
                        rating integer not null,
                        comment varchar(1000),
                        employer_id uuid not null,
                        provider_id uuid not null,
                        created_at timestamp not null
                    )
                    """);
            jdbcTemplate.execute("alter table review add column if not exists booking_id uuid");
            jdbcTemplate.execute("alter table review add column if not exists rating integer not null default 5");
            jdbcTemplate.execute("alter table review add column if not exists comment varchar(1000)");
            jdbcTemplate.execute("alter table review add column if not exists employer_id uuid");
            jdbcTemplate.execute("alter table review add column if not exists provider_id uuid");
            jdbcTemplate.execute("alter table review add column if not exists created_at timestamp not null default current_timestamp");
            jdbcTemplate.execute("""
                    create table if not exists notification (
                        id uuid primary key,
                        type varchar(80) not null,
                        title varchar(180) not null,
                        message varchar(1000) not null,
                        booking_id uuid,
                        recipient_id uuid,
                        read boolean not null default false,
                        created_at timestamp not null
                    )
                    """);
            jdbcTemplate.execute("alter table notification add column if not exists type varchar(80) not null default 'INFO'");
            jdbcTemplate.execute("alter table notification add column if not exists title varchar(180) not null default 'Notification'");
            jdbcTemplate.execute("alter table notification add column if not exists message varchar(1000) not null default ''");
            jdbcTemplate.execute("alter table notification add column if not exists booking_id uuid");
            jdbcTemplate.execute("alter table notification add column if not exists recipient_id uuid");
            jdbcTemplate.execute("alter table notification add column if not exists read boolean not null default false");
            jdbcTemplate.execute("alter table notification add column if not exists created_at timestamp not null default current_timestamp");
            ensureAdminAccount(jdbcTemplate, passwordEncoder, adminEmail, adminPhone, adminPassword);
        };
    }

    private void ensureAdminAccount(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            String adminEmail,
            String adminPhone,
            String adminPassword
    ) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }

        Integer existingAdmins = jdbcTemplate.queryForObject(
                "select count(*) from app_user where lower(email) = ?",
                Integer.class,
                adminEmail.trim().toLowerCase(Locale.ROOT)
        );
        if (existingAdmins != null && existingAdmins > 0) {
            return;
        }

        jdbcTemplate.update("""
                        insert into app_user (
                            id, full_name, email, phone, password_hash, role,
                            email_verified, active, archived, created_at, updated_at
                        )
                        values (?, 'Admin Jaankha', ?, ?, ?, 'ADMIN', true, true, false, current_timestamp, current_timestamp)
                        """,
                UUID.randomUUID(),
                adminEmail.trim().toLowerCase(Locale.ROOT),
                adminPhone.trim(),
                passwordEncoder.encode(adminPassword)
        );
    }
}
