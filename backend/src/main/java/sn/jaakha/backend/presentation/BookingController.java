package sn.jaakha.backend.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.BookingService;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.dto.BookingCreateRequest;
import sn.jaakha.backend.application.dto.BookingResponse;
import sn.jaakha.backend.application.dto.StatusUpdateRequest;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    public BookingController(BookingService bookingService, CurrentUserService currentUserService) {
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public BookingResponse create(
            HttpServletRequest request,
            @Valid @RequestBody BookingCreateRequest payload
    ) {
        return bookingService.create(currentUserService.requireUser(request), payload);
    }

    @GetMapping("/me")
    public List<BookingResponse> mine(HttpServletRequest request) {
        return bookingService.mine(currentUserService.requireUser(request));
    }

    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(
            HttpServletRequest request,
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest payload
    ) {
        return bookingService.updateStatus(currentUserService.requireUser(request), id, payload.status());
    }
}
