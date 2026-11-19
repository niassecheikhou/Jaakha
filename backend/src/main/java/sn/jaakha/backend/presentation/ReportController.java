package sn.jaakha.backend.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.ReportService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.ReportRequest;
import sn.jaakha.backend.application.dto.ReportResponse;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final UserAccess userAccess;

    public ReportController(ReportService reportService, UserAccess userAccess) {
        this.reportService = reportService;
        this.userAccess = userAccess;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @Valid @RequestBody ReportRequest request) {
        return reportService.create(userAccess.requireUser(principal), request);
    }
}
