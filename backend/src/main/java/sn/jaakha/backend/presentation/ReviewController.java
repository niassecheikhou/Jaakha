package sn.jaakha.backend.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.ReviewService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.ReviewRequest;
import sn.jaakha.backend.application.dto.ReviewResponse;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final UserAccess userAccess;

    public ReviewController(ReviewService reviewService, UserAccess userAccess) {
        this.reviewService = reviewService;
        this.userAccess = userAccess;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(userAccess.requireUser(principal), request);
    }
}
