package sn.jaakha.backend.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sn.jaakha.backend.presentation.ConflictException;
import sn.jaakha.backend.presentation.ForbiddenException;

class BookingStatusPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "PRESTATAIRE, REQUESTED, ACCEPTED",
            "PRESTATAIRE, ACCEPTED, IN_PROGRESS",
            "PRESTATAIRE, IN_PROGRESS, DONE",
            "EMPLOYEUR, REQUESTED, CANCELLED",
            "EMPLOYEUR, ACCEPTED, CANCELLED",
            "ADMIN, IN_PROGRESS, CANCELLED",
            "ADMIN, REQUESTED, ACCEPTED"
    })
    void allowsValidTransitions(String role, String current, String target) {
        assertThatCode(() -> BookingStatusPolicy.assertTransition(role, current, target))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "PRESTATAIRE, REQUESTED, DONE",
            "PRESTATAIRE, REQUESTED, IN_PROGRESS",
            "PRESTATAIRE, CANCELLED, ACCEPTED",
            "PRESTATAIRE, DONE, IN_PROGRESS",
            "ADMIN, DONE, CANCELLED",
            "ADMIN, CANCELLED, REQUESTED",
            "ADMIN, REQUESTED, DONE"
    })
    void rejectsTransitionsOutsideTheLifecycle(String role, String current, String target) {
        assertThatThrownBy(() -> BookingStatusPolicy.assertTransition(role, current, target))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @CsvSource({"IN_PROGRESS", "DONE", "CANCELLED"})
    void employerCannotCancelOnceMissionStartedOrClosed(String current) {
        assertThatThrownBy(() -> BookingStatusPolicy.assertTransition("EMPLOYEUR", current, "CANCELLED"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void employerCanOnlyCancel() {
        assertThatThrownBy(() -> BookingStatusPolicy.assertTransition("EMPLOYEUR", "IN_PROGRESS", "DONE"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void providerCannotCancel() {
        assertThatThrownBy(() -> BookingStatusPolicy.assertTransition("PRESTATAIRE", "REQUESTED", "CANCELLED"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void normalizeRejectsUnknownStatus() {
        assertThatThrownBy(() -> BookingStatusPolicy.normalize("PAID"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BookingStatusPolicy.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizeTrimsAndUppercases() {
        org.assertj.core.api.Assertions.assertThat(BookingStatusPolicy.normalize(" done ")).isEqualTo("DONE");
    }
}
