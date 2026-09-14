package sn.jaakha.backend.presentation;

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

    private final CurrentUserService currentUserService;

