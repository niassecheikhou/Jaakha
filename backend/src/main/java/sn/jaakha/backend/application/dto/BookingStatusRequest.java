package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import sn.jaakha.backend.domain.enums.BookingStatus;

public record BookingStatusRequest(@NotNull BookingStatus status) {
}
