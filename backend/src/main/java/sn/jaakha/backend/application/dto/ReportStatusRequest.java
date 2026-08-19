package sn.jaakha.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import sn.jaakha.backend.domain.enums.ReportStatus;

public record ReportStatusRequest(@NotNull ReportStatus status) {
}
