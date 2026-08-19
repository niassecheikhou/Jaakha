package sn.jaakha.backend.application.dto;

import java.time.Instant;
import java.util.UUID;
import sn.jaakha.backend.domain.model.Review;

public record ReviewResponse(
        UUID id,
        int rating,
        String comment,
        UUID employerId,
        String employerName,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getEmployer().getId(),
                review.getEmployer().getFullName(),
                review.getCreatedAt()
        );
    }
}
