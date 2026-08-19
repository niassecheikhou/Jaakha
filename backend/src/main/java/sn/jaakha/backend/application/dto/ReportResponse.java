package sn.jaakha.backend.application.dto;

import java.time.Instant;
import java.util.UUID;
import sn.jaakha.backend.domain.enums.ReportStatus;
import sn.jaakha.backend.domain.model.Report;

public record ReportResponse(
        UUID id,
        UUID reporterId,
        UUID bookingId,
        String reason,
        String details,
        ReportStatus status,
        Instant createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getBooking() == null ? null : report.getBooking().getId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
