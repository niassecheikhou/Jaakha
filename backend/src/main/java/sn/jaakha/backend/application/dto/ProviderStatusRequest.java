package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import sn.jaakha.backend.domain.enums.ProviderStatus;

public record ProviderStatusRequest(@NotNull ProviderStatus status) {
}
