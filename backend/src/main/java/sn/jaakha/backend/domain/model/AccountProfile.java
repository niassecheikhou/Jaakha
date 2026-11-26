package sn.jaakha.backend.domain.model;

import sn.jaakha.backend.domain.enums.IdentityDocumentSide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_profile")
public class AccountProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ownerKey;

    @Column(length = 1000)
    private String photoUrl;

    /** Recto de la piece d'identite (nom du fichier dans le stockage prive). */
    @Column(length = 1000)
    private String identityPhotoUrl;

    /** Verso de la piece d'identite (nom du fichier dans le stockage prive). */
    @Column(length = 1000)
    private String identityPhotoBackUrl;

    @Column(length = 1200)
    private String bio;

    @Column(length = 160)
    private String zone;

    // Adresse precise (exigee pour valider un compte administrateur).
    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyRate;

    private Integer experienceYears;

    @Column(length = 1000)
    private String skills;

    @Column(length = 160)
    private String guardianFullName;

    @Column(length = 40)
    private String guardianPhone;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AccountProfile() {
    }

    public AccountProfile(String ownerKey) {
        this.ownerKey = ownerKey;
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

    public String getOwnerKey() {
        return ownerKey;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getIdentityPhotoUrl() {
        return identityPhotoUrl;
    }

    public void setIdentityPhotoUrl(String identityPhotoUrl) {
        this.identityPhotoUrl = identityPhotoUrl;
    }

    public String getIdentityPhotoBackUrl() {
        return identityPhotoBackUrl;
    }

    public void setIdentityPhotoBackUrl(String identityPhotoBackUrl) {
        this.identityPhotoBackUrl = identityPhotoBackUrl;
    }

    public String getIdentityPhoto(IdentityDocumentSide side) {
        return side == IdentityDocumentSide.FRONT ? identityPhotoUrl : identityPhotoBackUrl;
    }

    public void setIdentityPhoto(IdentityDocumentSide side, String storedFilename) {
        if (side == IdentityDocumentSide.FRONT) {
            identityPhotoUrl = storedFilename;
        } else {
            identityPhotoBackUrl = storedFilename;
        }
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(BigDecimal monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public int getExperienceYears() {
        return experienceYears == null ? 0 : experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getGuardianFullName() {
        return guardianFullName;
    }

    public void setGuardianFullName(String guardianFullName) {
        this.guardianFullName = guardianFullName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public void setGuardianPhone(String guardianPhone) {
        this.guardianPhone = guardianPhone;
    }
}
