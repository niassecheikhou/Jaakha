package sn.jaakha.backend.application.dto;

import java.util.UUID;

public record ReviewResponse(
        UUID id,
        int rating,
        String comment,
        UUID employerId,
        String employerName,

) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
