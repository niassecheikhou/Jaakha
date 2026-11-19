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
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ReviewResponse create(
            HttpServletRequest request,
            @Valid @RequestBody ReviewCreateRequest payload
    ) {
        return reviewService.create(currentUserService.requireUser(request), payload);
    }
}
