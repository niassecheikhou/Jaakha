package sn.jaakha.backend.presentation;

import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.ReviewService;
import sn.jaakha.backend.application.dto.ReviewCreateRequest;
import sn.jaakha.backend.application.dto.ReviewResponse;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final AuditLogService auditLogService;
    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ReviewResponse create(
            HttpServletRequest request,
            @Valid @RequestBody ReviewCreateRequest payload
    ) {
        AuthenticatedUser user = currentUserService.requireUser(request);
        ReviewResponse review = reviewService.create(user, payload);
        auditLogService.record(user, AuditLogService.REVIEW_CREATED, "RESERVATION", payload.bookingId(), "Avis sur une prestation",
                "Note : " + payload.rating() + "/5");
        return review;
    }
}
