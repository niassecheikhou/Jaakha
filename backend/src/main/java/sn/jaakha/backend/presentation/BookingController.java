package sn.jaakha.backend.presentation;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.BookingService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.BookingRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.BookingStatusRequest;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final UserAccess userAccess;

    public BookingController(BookingService bookingService, UserAccess userAccess) {
        this.bookingService = bookingService;
        this.userAccess = userAccess;
    }

    @PostMapping
    public BookingResponse create(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @Valid @RequestBody BookingRequest request) {
        return bookingService.create(userAccess.requireUser(principal), request);
    }

    @GetMapping("/me")
    public List<BookingResponse> mine(@AuthenticationPrincipal JaakhaUserPrincipal principal) {
        return bookingService.listMine(userAccess.requireUser(principal));
    }

    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(@AuthenticationPrincipal JaakhaUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.updateStatus(userAccess.requireUser(principal), id, request.status());
    }
}
