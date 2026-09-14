package sn.jaakha.backend.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.jaakha.backend.application.AccountProfileService;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.dto.AccountProfileRequest;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.PhotoUploadResponse;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@RestController
@RequestMapping("/api/account/me")
public class AccountProfileController {
    private final AccountProfileService accountProfileService;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public AccountProfileController(
            AccountProfileService accountProfileService,
            StorageService storageService,
            CurrentUserService currentUserService
    ) {
        this.accountProfileService = accountProfileService;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public AccountProfileResponse mine(HttpServletRequest request) {
        return accountProfileService.getMine(currentUserService.requireUser(request).id().toString());
    }

    @PutMapping
    public AccountProfileResponse saveMine(
            HttpServletRequest request,
            @Valid @RequestBody AccountProfileRequest payload
    ) {
        return accountProfileService.saveMine(currentUserService.requireUser(request).id().toString(), payload);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoUploadResponse uploadPhoto(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        AuthenticatedUser user = currentUserService.requireUser(request);
        String relativeUrl = storageService.storeAccountPhoto(file);
        AccountProfileResponse profile = accountProfileService.updatePhoto(
                user.id().toString(),
                absoluteUrl(relativeUrl)
        );
        return new PhotoUploadResponse(profile.photoUrl());
    }

    @PostMapping(value = "/identity-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoUploadResponse uploadIdentityPhoto(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        AuthenticatedUser user = currentUserService.requireUser(request);
        String relativeUrl = storageService.storeIdentityPhoto(file);
        AccountProfileResponse profile = accountProfileService.updateIdentityPhoto(
                user.id().toString(),
                absoluteUrl(relativeUrl)
        );
        return new PhotoUploadResponse(profile.identityPhotoUrl());
    }

    private String absoluteUrl(String relativeUrl) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
    }
}
