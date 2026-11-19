package sn.jaakha.backend.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.ReviewRequest;
import sn.jaakha.backend.application.dto.ReviewResponse;
import sn.jaakha.backend.domain.enums.BookingStatus;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.Booking;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.model.Review;
import sn.jaakha.backend.domain.repository.BookingRepository;
import sn.jaakha.backend.domain.repository.ProviderProfileRepository;
import sn.jaakha.backend.domain.repository.ReviewRepository;

@Service
public class ReviewService {
    private final BookingRepository bookingRepository;
    private final ProviderProfileRepository providerRepository;
    private final ReviewRepository reviewRepository;

    public ReviewService(BookingRepository bookingRepository, ProviderProfileRepository providerRepository,
            ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.providerRepository = providerRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewResponse create(AppUser employer, ReviewRequest request) {
        if (employer.getRole() != Role.EMPLOYEUR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seuls les employeurs peuvent laisser un avis.");
        }
        if (request.bookingId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La reservation est requise.");
        }
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation introuvable."));
        if (!booking.getEmployer().getId().equals(employer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        if (booking.getStatus() != BookingStatus.DONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un avis est possible uniquement apres une mission terminee.");
        }
        if (reviewRepository.findByBooking(booking).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Un avis existe deja pour cette reservation.");
        }

        ProviderProfile provider = booking.getProvider();
        Review review = reviewRepository.save(new Review(booking, employer, provider, request.rating(), request.comment()));
        updateProviderRating(provider, request.rating());
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForProvider(UUID providerId) {
        ProviderProfile provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prestataire introuvable."));
        return reviewRepository.findByProviderOrderByCreatedAtDesc(provider).stream().map(ReviewResponse::from).toList();
    }

    private void updateProviderRating(ProviderProfile provider, int rating) {
        int oldCount = provider.getReviewCount();
        BigDecimal total = provider.getAverageRating().multiply(BigDecimal.valueOf(oldCount)).add(BigDecimal.valueOf(rating));
        int newCount = oldCount + 1;
        provider.setReviewCount(newCount);
        provider.setAverageRating(total.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP));
        providerRepository.save(provider);
    }
}
