package sn.jaakha.backend.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AdminBookingCreateRequest;
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingAdminRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingRepository;
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
    private final BookingRepository bookingRepository;

    public AdminService(
            BookingAdminRepository bookingAdminRepository,
            PasswordEncoder passwordEncoder,
            ProviderProfileRepository providerProfileRepository,
            UserAccountRepository userAccountRepository,
            MailService mailService,
            NotificationService notificationService,
            AuthService authService,
            BookingRepository bookingRepository
    ) {
        this.bookingAdminRepository = bookingAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.providerProfileRepository = providerProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.authService = authService;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Le super administrateur voit tous les comptes ; un administrateur ne voit que les employeurs
     * et les prestataires qu'il a lui-meme crees.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> users(AuthenticatedUser actor) {
        if (Roles.isSuperAdmin(actor.role())) {
            return userAccountRepository.findAll();
        }
        return userAccountRepository.findAllCreatedBy(actor.id()).stream()
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
    public List<ProviderProfileResponse> providers(AuthenticatedUser actor, String status) {
        List<ProviderProfile> profiles;
        if (status == null || status.isBlank()) {
            profiles = providerProfileRepository.findAll();
        } else {
            profiles = providerProfileRepository.findByStatusOrderByCreatedAtDesc(ProviderStatus.valueOf(status));
        }
        if (!Roles.isSuperAdmin(actor.role())) {
            Set<UUID> managedUserIds = userAccountRepository.findIdsCreatedBy(actor.id());
            profiles = profiles.stream()
                    .filter(profile -> managedUserIds.contains(profile.getUserId()))
                    .toList();
        }
        return profiles.stream()
                .map(ProviderProfileResponse::from)
                .toList();
    }

    @Transactional
    public ProviderProfileResponse updateProviderStatus(AuthenticatedUser actor, UUID id, String status) {
        ProviderProfile profile = providerProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Le profil prestataire est introuvable."));
        assertManagesUser(actor, profile.getUserId());
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
    public List<BookingResponse> bookings(AuthenticatedUser actor, String status) {
        if (Roles.isSuperAdmin(actor.role())) {
            return bookingAdminRepository.findAll(status);
        }
        return bookingAdminRepository.findAllManagedBy(actor.id(), status);
    }

    @Transactional
    public BookingResponse updateBookingStatus(AuthenticatedUser actor, UUID id, String status) {
        String targetStatus = BookingStatusPolicy.normalize(status);
        String currentStatus = bookingAdminRepository.findStatus(id)
                .orElseThrow(() -> new IllegalArgumentException("La reservation est introuvable."));
        if (!Roles.isSuperAdmin(actor.role()) && !bookingAdminRepository.isManagedBy(id, actor.id())) {
            throw new ForbiddenException("Cette reservation ne concerne aucun des comptes que tu as crees.");
        }
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
     * - un compte EMPLOYEUR ou PRESTATAIRE est gere par l'administrateur qui l'a cree
     *   (et par le super administrateur, qui gere tous les comptes).
     */
    /**
     * Reservation creee par un administrateur pour le compte d'un employeur qu'il gere (le super
     * administrateur : n'importe quel employeur). Le prestataire doit etre valide et visible, comme
     * pour une reservation faite par l'employeur. Avec validate, elle est acceptee aussitot.
     */
    @Transactional
    public BookingResponse createBooking(AuthenticatedUser actor, AdminBookingCreateRequest request) {
        UserResponse employer = userAccountRepository.findById(request.employerId())
                .orElseThrow(() -> new IllegalArgumentException("L'employeur est introuvable."));
        if (!Roles.EMPLOYEUR.equals(employer.role())) {
            throw new IllegalArgumentException("Une reservation se fait pour le compte d'un employeur.");
        }
        assertManagesUser(actor, employer.id());
        if (!employer.active() || employer.archived()) {
            throw new IllegalArgumentException("Le compte de cet employeur doit etre valide avant de reserver pour lui.");
        }
        ProviderProfile provider = providerProfileRepository.findById(request.providerId())
                .filter(profile -> profile.getStatus() == ProviderStatus.APPROVED)
                .filter(profile -> userAccountRepository.visibleProviderUserIds().contains(profile.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Ce prestataire n'est pas disponible pour une reservation."));

        BookingResponse booking = bookingRepository.create(
                employer.id(),
                provider.getId(),
                request.serviceDate(),
                request.startTime(),
                request.durationHours(),
                request.address().trim(),
                request.instructions() == null || request.instructions().isBlank() ? null : request.instructions().trim()
        );
        if (!request.validate()) {
            notificationService.notifyAdminsAboutBooking(booking);
            notificationService.notifyProviderAboutBookingRequested(booking);
            return booking;
        }
        BookingStatusPolicy.assertTransition(Roles.ADMIN, booking.status(), "ACCEPTED");
        BookingResponse accepted = bookingAdminRepository.updateStatus(booking.id(), booking.status(), "ACCEPTED");
        notificationService.notifyParticipantsAboutBookingStatus(accepted);
        return accepted;
    }

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
            return user;
        }
        assertManagesUser(actor, id);
        return user;
    }

    /**
     * Verifie qu'un administrateur peut consulter le compte (fiche, piece d'identite).
     * Le super administrateur a acces a tous les comptes.
     */
    @Transactional(readOnly = true)
    public void assertCanViewUser(AuthenticatedUser actor, UUID userId) {
        if (Roles.isSuperAdmin(actor.role())) {
            return;
        }
        UserResponse user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Le compte utilisateur est introuvable."));
        if (Roles.isAdministration(user.role())) {
            throw new ForbiddenException("Seul le super administrateur peut consulter un compte d'administration.");
        }
        assertManagesUser(actor, userId);
    }

    /** Un ADMIN ne gere que les comptes qu'il a crees ; le super administrateur gere tout. */
    private void assertManagesUser(AuthenticatedUser actor, UUID userId) {
        if (Roles.isSuperAdmin(actor.role())) {
            return;
        }
        boolean createdByActor = userAccountRepository.findCreatedBy(userId)
                .map(creator -> creator.equals(actor.id()))
                .orElse(false);
        if (!createdByActor) {
            throw new ForbiddenException("Tu ne peux gerer que les comptes que tu as crees.");
        }
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
