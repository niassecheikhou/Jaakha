package sn.jaakha.backend.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingAdminRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;
import sn.jaakha.backend.presentation.ConflictException;
import sn.jaakha.backend.presentation.ForbiddenException;

@Service
public class AdminService {
    private final BookingAdminRepository bookingAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProviderProfileRepository providerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final AuthService authService;

    public AdminService(
            BookingAdminRepository bookingAdminRepository,
            PasswordEncoder passwordEncoder,
            ProviderProfileRepository providerProfileRepository,
            UserAccountRepository userAccountRepository,
            MailService mailService,
            NotificationService notificationService,
            AuthService authService
    ) {
        this.bookingAdminRepository = bookingAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.providerProfileRepository = providerProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    /**
     * Le super administrateur voit tous les comptes ; un administrateur ne voit que les employeurs
     * et les prestataires (les comptes d'administration ne le concernent pas).
     */
    @Transactional(readOnly = true)
    public List<UserResponse> users(AuthenticatedUser actor) {
        List<UserResponse> users = userAccountRepository.findAll();
        if (Roles.isSuperAdmin(actor.role())) {
            return users;
        }
        return users.stream()
                .filter(user -> !Roles.isAdministration(user.role()))
                .toList();
    }

    @Transactional
    public UserResponse createUser(AuthenticatedUser actor, AdminCreateUserRequest request) {
        UUID userId = UUID.randomUUID();
        String role = request.role() == null || request.role().isBlank() ? Roles.EMPLOYEUR : request.role();
        boolean adminAccount = Roles.ADMIN.equals(role);
        if (adminAccount && !Roles.isSuperAdmin(actor.role())) {
            throw new ForbiddenException("Seul le super administrateur peut creer un compte administrateur.");
        }

        try {
            userAccountRepository.create(
                    userId,
                    request.fullName().trim(),
                    request.email().trim().toLowerCase(Locale.ROOT),
                    request.phone().trim(),
                    passwordEncoder.encode(request.password()),
                    role,
                    // Tout nouveau compte (administrateur compris) attend la validation de son dossier.
                    false,
                    actor.id()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Un compte existe deja avec cet email ou ce numero de telephone.");
        }

        UserResponse user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Le compte utilisateur cree est introuvable."));
        mailService.sendAccountCreated(user, authService.issuePasswordResetUrl(user.id()));
        return user;
    }

    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> providers(String status) {
        List<ProviderProfile> profiles;
        if (status == null || status.isBlank()) {
            profiles = providerProfileRepository.findAll();
        } else {
            profiles = providerProfileRepository.findByStatusOrderByCreatedAtDesc(ProviderStatus.valueOf(status));
        }
        return profiles.stream()
                .map(ProviderProfileResponse::from)
                .toList();
    }

    @Transactional
    public ProviderProfileResponse updateProviderStatus(UUID id, String status) {
        ProviderProfile profile = providerProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Le profil prestataire est introuvable."));
        profile.setStatus(ProviderStatus.valueOf(status));
        return ProviderProfileResponse.from(providerProfileRepository.save(profile));
    }

    @Transactional
    public UserResponse updateUserActive(AuthenticatedUser actor, UUID id, boolean active) {
        UserResponse user = findEditableUser(actor, id);
        if (user.archived()) {
            throw new IllegalArgumentException("Ce compte est archive. Restaure-le avant de modifier sa validation.");
        }
        if (active) {
            assertProfileReadyForValidation(user);
        }

        userAccountRepository.updateActive(id, active);
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Le compte utilisateur est introuvable."));
    }

    @Transactional
    public UserResponse updateUserArchived(AuthenticatedUser actor, UUID id, boolean archived) {
        findEditableUser(actor, id);
        userAccountRepository.updateArchived(id, archived);
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Le compte utilisateur est introuvable."));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> bookings(String status) {
        return bookingAdminRepository.findAll(status);
    }

    @Transactional
    public BookingResponse updateBookingStatus(UUID id, String status) {
        String targetStatus = BookingStatusPolicy.normalize(status);
        String currentStatus = bookingAdminRepository.findStatus(id)
                .orElseThrow(() -> new IllegalArgumentException("La reservation est introuvable."));
        BookingStatusPolicy.assertTransition("ADMIN", currentStatus, targetStatus);
        BookingResponse booking = bookingAdminRepository.updateStatus(id, currentStatus, targetStatus);
        notificationService.notifyParticipantsAboutBookingStatus(booking);
        return booking;
    }

    /**
     * Regles de gestion des comptes :
     * - un compte SUPER_ADMIN n'est jamais modifiable depuis le dashboard ;
     * - un compte ADMIN n'est modifiable que par le super administrateur qui l'a cree
     *   (ou par tout super administrateur s'il a ete cree avant l'arrivee de ce role) ;
     * - les comptes EMPLOYEUR et PRESTATAIRE sont geres par tout administrateur.
     */
    private UserResponse findEditableUser(AuthenticatedUser actor, UUID id) {
        UserResponse user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Le compte utilisateur est introuvable."));
        if (Roles.isSuperAdmin(user.role())) {
            throw new ForbiddenException("Le compte super administrateur ne peut pas etre modifie depuis ce dashboard.");
        }
        if (Roles.ADMIN.equals(user.role())) {
            if (!Roles.isSuperAdmin(actor.role())) {
                throw new ForbiddenException("Seul le super administrateur peut gerer un compte administrateur.");
            }
            boolean createdByAnotherSuperAdmin = userAccountRepository.findCreatedBy(id)
                    .map(creator -> !creator.equals(actor.id()))
                    .orElse(false);
            if (createdByAnotherSuperAdmin) {
                throw new ForbiddenException("Tu ne peux gerer que les administrateurs que tu as crees.");
            }
        }
        return user;
    }

    private void assertProfileReadyForValidation(UserResponse user) {
        List<String> missingFields = new ArrayList<>();
        addMissing(missingFields, user.fullName(), "nom complet");
        addMissing(missingFields, user.phone(), "telephone");
        if (Roles.ADMIN.equals(user.role())) {
            // Dossier administrateur : piece d'identite recto/verso, adresse et localisation.
            addMissing(missingFields, user.identityPhotoUrl(), "recto de la piece d'identite");
            addMissing(missingFields, user.identityPhotoBackUrl(), "verso de la piece d'identite");
            addMissing(missingFields, user.address(), "adresse");
            addMissing(missingFields, user.zone(), "localisation (zone)");
        } else {
            addMissing(missingFields, user.photoUrl(), "photo du profil");
            addMissing(missingFields, user.identityPhotoUrl(), "recto de la piece d'identite");
            addMissing(missingFields, user.identityPhotoBackUrl(), "verso de la piece d'identite");
            addMissing(missingFields, user.zone(), "zone");
        }

        if ("PRESTATAIRE".equals(user.role())) {
            addMissing(missingFields, user.bio(), "bio");
            if (user.monthlyRate() == null || user.monthlyRate().compareTo(BigDecimal.ZERO) <= 0) {
                missingFields.add("tarif mensuel");
            }
            if (user.skills() == null || user.skills().isEmpty()) {
                missingFields.add("competences");
            }
            addMissing(missingFields, user.guardianFullName(), "nom complet du tuteur");
            addMissing(missingFields, user.guardianPhone(), "telephone du tuteur");
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Impossible de valider ce compte: dossier incomplet. Champs manquants: "
                            + String.join(", ", missingFields)
                            + "."
            );
        }
    }

    private void addMissing(List<String> missingFields, String value, String label) {
        if (value == null || value.isBlank()) {
            missingFields.add(label);
        }
    }
}
