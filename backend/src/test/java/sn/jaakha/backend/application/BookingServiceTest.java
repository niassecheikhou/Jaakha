package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.infrastructure.persistence.BookingRepository;
import sn.jaakha.backend.infrastructure.persistence.UserAccountRepository;
import sn.jaakha.backend.presentation.ConflictException;
import sn.jaakha.backend.presentation.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final UUID BOOKING_ID = UUID.randomUUID();

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ProviderProfileRepository providerProfileRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingService bookingService;

    private final AuthenticatedUser provider = new AuthenticatedUser(UUID.randomUUID(), "Awa Diop", "PRESTATAIRE");
    private final AuthenticatedUser employer = new AuthenticatedUser(UUID.randomUUID(), "Moussa Fall", "EMPLOYEUR");

    @Test
    void providerCannotJumpFromRequestedToDone() {
        when(bookingRepository.findStatusOwnedBy(BOOKING_ID, provider.id(), "PRESTATAIRE"))
                .thenReturn(Optional.of("REQUESTED"));

        assertThatThrownBy(() -> bookingService.updateStatus(provider, BOOKING_ID, "DONE"))
                .isInstanceOf(ConflictException.class);
        verify(bookingRepository, never()).updateStatus(any(), anyString(), anyString());
    }

    @Test
    void providerCannotReviveCancelledBooking() {
        when(bookingRepository.findStatusOwnedBy(BOOKING_ID, provider.id(), "PRESTATAIRE"))
                .thenReturn(Optional.of("CANCELLED"));

        assertThatThrownBy(() -> bookingService.updateStatus(provider, BOOKING_ID, "ACCEPTED"))
                .isInstanceOf(ConflictException.class);
        verify(bookingRepository, never()).updateStatus(any(), anyString(), anyString());
    }

    @Test
    void employerCannotCancelCompletedBooking() {
        when(bookingRepository.findStatusOwnedBy(BOOKING_ID, employer.id(), "EMPLOYEUR"))
                .thenReturn(Optional.of("DONE"));

        assertThatThrownBy(() -> bookingService.updateStatus(employer, BOOKING_ID, "CANCELLED"))
                .isInstanceOf(ConflictException.class);
        verify(bookingRepository, never()).updateStatus(any(), anyString(), anyString());
    }

    @Test
    void bookingOfAnotherUserIsNotFound() {
        when(bookingRepository.findStatusOwnedBy(BOOKING_ID, employer.id(), "EMPLOYEUR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateStatus(employer, BOOKING_ID, "CANCELLED"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validTransitionUpdatesFromCurrentStatus() {
        when(bookingRepository.findStatusOwnedBy(BOOKING_ID, provider.id(), "PRESTATAIRE"))
                .thenReturn(Optional.of("ACCEPTED"));

        bookingService.updateStatus(provider, BOOKING_ID, "in_progress");

        verify(bookingRepository).updateStatus(BOOKING_ID, "ACCEPTED", "IN_PROGRESS");
    }

    @Test
    void adminCannotUseParticipantEndpoint() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "Admin", "ADMIN");

        assertThatThrownBy(() -> bookingService.updateStatus(admin, BOOKING_ID, "DONE"))
                .isInstanceOf(ForbiddenException.class);
    }
}
