package sn.jaakha.backend.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.BookingRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.domain.enums.BookingStatus;
import sn.jaakha.backend.domain.enums.NotificationType;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.Booking;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.BookingRepository;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ProviderProfileRepository providerRepository;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, ProviderProfileRepository providerRepository,
            NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.providerRepository = providerRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponse create(AppUser employer, BookingRequest request) {
        if (employer.getRole() != Role.EMPLOYEUR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seuls les employeurs peuvent reserver.");
        }
        ProviderProfile provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prestataire introuvable."));
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce prestataire n'est pas encore valide.");
        }

        Booking booking = bookingRepository.save(new Booking(
                employer,
                provider,
                request.serviceDate(),
                request.startTime(),
                request.durationHours(),
                request.address().trim(),
                request.instructions()
        ));
        notificationService.notifyAdmins(
                NotificationType.BOOKING_REQUESTED,
                "Nouvelle demande de reservation",
                employer.getFullName() + " a demande une reservation avec " + provider.getUser().getFullName() + ".",
                booking.getId(),
                employer.getId()
        );
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMine(AppUser user) {
        if (user.getRole() == Role.EMPLOYEUR) {
            return bookingRepository.findByEmployerOrderByCreatedAtDesc(user).stream().map(BookingResponse::from).toList();
        }
        if (user.getRole() == Role.PRESTATAIRE) {
            ProviderProfile provider = providerRepository.findByUser(user)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil prestataire introuvable."));
            return bookingRepository.findByProviderOrderByCreatedAtDesc(provider).stream().map(BookingResponse::from).toList();
        }
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listAll(BookingStatus status) {
        List<Booking> bookings = status == null
                ? bookingRepository.findAllByOrderByCreatedAtDesc()
                : bookingRepository.findByStatusOrderByCreatedAtDesc(status);
        return bookings.stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse updateStatus(AppUser actor, UUID bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation introuvable."));
        authorizeStatusChange(actor, booking, status);
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);
        notificationService.notifyAdmins(
                NotificationType.BOOKING_UPDATED,
                "Reservation mise a jour",
                "La reservation " + booking.getId() + " est maintenant " + status + ".",
                booking.getId(),
                actor.getId()
        );
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Booking getAccessibleBooking(AppUser actor, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation introuvable."));
        if (actor.getRole() == Role.ADMIN || booking.getEmployer().getId().equals(actor.getId())
                || booking.getProvider().getUser().getId().equals(actor.getId())) {
            return booking;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse.");
    }

    private void authorizeStatusChange(AppUser actor, Booking booking, BookingStatus next) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        boolean isEmployer = booking.getEmployer().getId().equals(actor.getId());
        boolean isProvider = booking.getProvider().getUser().getId().equals(actor.getId());
        if (isEmployer && next == BookingStatus.CANCELLED
                && (booking.getStatus() == BookingStatus.REQUESTED || booking.getStatus() == BookingStatus.ACCEPTED)) {
            return;
        }
        if (isProvider && isProviderTransitionAllowed(booking.getStatus(), next)) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Changement de statut non autorise.");
    }

    private boolean isProviderTransitionAllowed(BookingStatus current, BookingStatus next) {
        return (current == BookingStatus.REQUESTED && (next == BookingStatus.ACCEPTED || next == BookingStatus.CANCELLED))
                || (current == BookingStatus.ACCEPTED && next == BookingStatus.IN_PROGRESS)
                || (current == BookingStatus.IN_PROGRESS && next == BookingStatus.DONE);
    }
}
package sn.jaakha.backend.application;

import java.util.List;
import java.util.Set;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.BookingCreateRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.domain.enums.ProviderStatus;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;
import sn.jaakha.backend.presentation.ForbiddenException;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            ProviderProfileRepository providerProfileRepository,
            UserAccountRepository userAccountRepository,
            NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponse create(AuthenticatedUser user, BookingCreateRequest request) {
        requireRole(user, "EMPLOYEUR", "Seuls les employeurs peuvent creer une reservation.");
        ProviderProfile provider = providerProfileRepository.findById(request.providerId())
                .filter(profile -> profile.getStatus() == ProviderStatus.APPROVED)
                .filter(profile -> userAccountRepository.visibleProviderUserIds().contains(profile.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Ce prestataire n'est pas disponible pour une reservation."));

        BookingResponse booking = bookingRepository.create(user.id(), provider.getId());
        notificationService.notifyAdminsAboutBooking(booking);
        notificationService.notifyProviderAboutBookingRequested(booking);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(AuthenticatedUser user) {
        if (!"EMPLOYEUR".equals(user.role()) && !"PRESTATAIRE".equals(user.role())) {
            throw new ForbiddenException("Tu dois etre connecte avec un compte employeur ou prestataire.");
        }
        return bookingRepository.findMine(user.id(), user.role());
    }

    @Transactional
    public BookingResponse updateStatus(AuthenticatedUser user, UUID id, String status) {
        String targetStatus = BookingStatusPolicy.normalize(status);
        if (!"EMPLOYEUR".equals(user.role()) && !"PRESTATAIRE".equals(user.role())) {
            throw new ForbiddenException("Tu dois etre connecte avec un compte employeur ou prestataire.");
        }
        String currentStatus = bookingRepository.findStatusOwnedBy(id, user.id(), user.role())
                .orElseThrow(() -> new IllegalArgumentException("La reservation est introuvable ou ne t'appartient pas."));
        BookingStatusPolicy.assertTransition(user.role(), currentStatus, targetStatus);
        return bookingRepository.updateStatus(id, currentStatus, targetStatus);
    }

    private void requireRole(AuthenticatedUser user, String role, String message) {
        if (!role.equals(user.role())) {
            throw new ForbiddenException(message);
        }
    }
}
