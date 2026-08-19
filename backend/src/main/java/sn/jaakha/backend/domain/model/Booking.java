package sn.jaakha.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.jaakha.backend.domain.enums.BookingStatus;

@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private AppUser employer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private int durationHours;

    @Column(nullable = false)
    private String address;

    @Column(length = 1200)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Booking() {
    }

    public Booking(AppUser employer, ProviderProfile provider, LocalDate serviceDate, LocalTime startTime,
            int durationHours, String address, String instructions) {
        this.employer = employer;
        this.provider = provider;
        this.serviceDate = serviceDate;
        this.startTime = startTime;
        this.durationHours = durationHours;
        this.address = address;
        this.instructions = instructions;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getEmployer() {
        return employer;
    }

    public ProviderProfile getProvider() {
        return provider;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public String getAddress() {
        return address;
    }

    public String getInstructions() {
        return instructions;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
