package sn.jaakha.backend.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.ReviewCreateRequest;
import sn.jaakha.backend.application.dto.ReviewResponse;
import sn.jaakha.backend.infrastructure.persistence.ReviewRepository;
import sn.jaakha.backend.presentation.ConflictException;
import sn.jaakha.backend.presentation.ForbiddenException;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewResponse create(AuthenticatedUser user, ReviewCreateRequest request) {
        if (!"EMPLOYEUR".equals(user.role())) {
            throw new ForbiddenException("Seuls les employeurs peuvent noter une reservation.");
        }
        if (reviewRepository.existsForBooking(request.bookingId())) {
            throw new ConflictException("Cette reservation a deja ete notee.");
        }

        ReviewRepository.ReviewableBooking booking = reviewRepository.findReviewableBooking(request.bookingId(), user.id());
        return reviewRepository.create(booking.id(), user.id(), booking.providerId(), request.rating(), request.comment());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByProvider(UUID providerId) {
        return reviewRepository.findByProvider(providerId);
    }
}
