package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.model.Booking;
import sn.jaakha.backend.domain.model.ProviderProfile;
import sn.jaakha.backend.domain.model.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByBooking(Booking booking);

    List<Review> findByProviderOrderByCreatedAtDesc(ProviderProfile provider);
}
