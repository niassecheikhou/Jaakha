package sn.jaakha.backend.application;

import java.util.List;
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
