package sn.jaakha.backend.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.ReportRequest;
import sn.jaakha.backend.application.dto.ReportResponse;
import sn.jaakha.backend.domain.enums.NotificationType;
import sn.jaakha.backend.domain.enums.ReportStatus;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.Booking;
import sn.jaakha.backend.domain.model.Report;
import sn.jaakha.backend.domain.repository.ReportRepository;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final BookingService bookingService;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository, BookingService bookingService,
            NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.bookingService = bookingService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportResponse create(AppUser reporter, ReportRequest request) {
        Booking booking = request.bookingId() == null ? null : bookingService.getAccessibleBooking(reporter, request.bookingId());
        Report report = reportRepository.save(new Report(reporter, booking, request.reason().trim(), request.details()));
        notificationService.notifyAdmins(
                NotificationType.REPORT_CREATED,
                "Nouveau signalement",
                reporter.getFullName() + " a cree un signalement: " + request.reason(),
                booking == null ? null : booking.getId(),
                reporter.getId()
        );
        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listAll(ReportStatus status) {
        List<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(ReportResponse::from).toList();
    }

    @Transactional
    public ReportResponse updateStatus(UUID id, ReportStatus status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Signalement introuvable."));
        report.setStatus(status);
        return ReportResponse.from(reportRepository.save(report));
    }
}
