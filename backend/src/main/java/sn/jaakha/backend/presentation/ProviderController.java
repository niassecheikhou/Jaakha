package sn.jaakha.backend.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.ReviewService;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.PhotoUploadResponse;
import sn.jaakha.backend.application.dto.ProviderProfileRequest;
import sn.jaakha.backend.application.dto.ProviderProfileResponse;
import sn.jaakha.backend.application.dto.ReviewResponse;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {
    private final ProviderService providerService;
    private final ReviewService reviewService;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public ProviderController(
            ProviderService providerService,
            ReviewService reviewService,
            StorageService storageService,
            CurrentUserService currentUserService
    ) {
        this.providerService = providerService;
        this.reviewService = reviewService;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ProviderProfileResponse> search(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return providerService.search(zone, skill, maxPrice);
    }

    @GetMapping("/me")
    public ProviderProfileResponse mine(HttpServletRequest request) {
        return providerService.getMine(currentUserService.requireUser(request));
    }

    @PutMapping("/me")
    public ProviderProfileResponse saveMine(
            HttpServletRequest request,
            @Valid @RequestBody ProviderProfileRequest payload
    ) {
        return providerService.saveMine(currentUserService.requireUser(request), payload);
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoUploadResponse uploadPhoto(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        // Authentification et role verifies AVANT d'ecrire le fichier sur le disque public.
        AuthenticatedUser user = currentUserService.requireUser(request);
        providerService.requireProvider(user);
        String relativeUrl = storageService.storeProviderPhoto(file);
        ProviderProfileResponse profile = providerService.updatePhoto(user, absoluteUrl(relativeUrl));
        return new PhotoUploadResponse(profile.photoUrl());
    }

    @GetMapping("/{id}")
    public ProviderProfileResponse detail(@PathVariable UUID id) {
        return providerService.getById(id);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable UUID id) {
        providerService.getById(id);
        return reviewService.findByProvider(id);
    }

    private String absoluteUrl(String relativeUrl) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
    }
}
