package sn.jaakha.backend.presentation;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.ReviewService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.ProviderProfileRequest;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.application.dto.ReviewResponse;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {
    private final ProviderService providerService;
    private final ReviewService reviewService;
    private final UserAccess userAccess;

    public ProviderController(ProviderService providerService, ReviewService reviewService, UserAccess userAccess) {
        this.providerService = providerService;
        this.reviewService = reviewService;
        this.userAccess = userAccess;
    }

    @GetMapping
    public List<ProviderProfileResponse> search(@RequestParam(required = false) String zone,
            @RequestParam(required = false) BigDecimal maxHourlyRate,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) DayOfWeek availableDay) {
        return providerService.search(zone, maxHourlyRate, minRating, skill, availableDay);
    }

    @GetMapping("/{id}")
    public ProviderProfileResponse detail(@PathVariable UUID id) {
        return providerService.getById(id);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable UUID id) {
        return reviewService.listForProvider(id);
    }

    @GetMapping("/me")
    public ProviderProfileResponse mine(@AuthenticationPrincipal JaakhaUserPrincipal principal) {
        return providerService.getMine(userAccess.requireUser(principal));
    }

    @PutMapping("/me")
    public ProviderProfileResponse upsertMine(@AuthenticationPrincipal JaakhaUserPrincipal principal,
            @Valid @RequestBody ProviderProfileRequest request) {
        return providerService.saveMyProfile(userAccess.requireUser(principal), request);
    }
}
