package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.enums.BookingStatus;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.model.Booking;
import sn.jaakha.backend.domain.model.ProviderProfile;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByEmployerOrderByCreatedAtDesc(AppUser employer);

    List<Booking> findByProviderOrderByCreatedAtDesc(ProviderProfile provider);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    List<Booking> findAllByOrderByCreatedAtDesc();
}
