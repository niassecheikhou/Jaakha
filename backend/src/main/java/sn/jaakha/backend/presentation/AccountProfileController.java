package sn.jaakha.backend.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import sn.jaakha.backend.application.ProviderService;
import sn.jaakha.backend.application.dto.AccountProfileRequest;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.application.dto.AuthenticatedUser;
import sn.jaakha.backend.application.dto.IdentityPhotosResponse;
import sn.jaakha.backend.application.dto.PhotoUploadResponse;
import sn.jaakha.backend.domain.enums.IdentityDocumentSide;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@RestController
@RequestMapping("/api/account/me")
public class AccountProfileController {
    private final AccountProfileService accountProfileService;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;
    private final ProviderService providerService;

    public AccountProfileController(
            AccountProfileService accountProfileService,
            StorageService storageService,
            CurrentUserService currentUserService,
            ProviderService providerService
    ) {
        this.accountProfileService = accountProfileService;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
        this.providerService = providerService;
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
        // Prestataire : la photo publique est mise a jour tout de suite, sans attendre l'enregistrement du dossier.
        providerService.syncAccountPhoto(user, profile.photoUrl());
        return new PhotoUploadResponse(profile.photoUrl());
    }

    /**
     * Depot du recto et/ou du verso de la piece d'identite : les deux faces en une fois,
     * ou une seule face (l'autre est conservee). Les deux fichiers sont valides avant toute ecriture.
     */
    @PostMapping(value = "/identity-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IdentityPhotosResponse uploadIdentityPhotos(
            HttpServletRequest request,
            @RequestParam(value = "front", required = false) MultipartFile front,
            @RequestParam(value = "back", required = false) MultipartFile back
    ) {
        AuthenticatedUser user = currentUserService.requireUser(request);
        Map<IdentityDocumentSide, MultipartFile> files = new EnumMap<>(IdentityDocumentSide.class);
        if (front != null) {
            files.put(IdentityDocumentSide.FRONT, front);
        }
        if (back != null) {
            files.put(IdentityDocumentSide.BACK, back);
        }
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Ajoute le recto et/ou le verso de la piece d'identite.");
        }
        files.values().forEach(storageService::validateImage);

        Map<IdentityDocumentSide, String> storedFilenames = new EnumMap<>(IdentityDocumentSide.class);
        files.forEach((side, file) -> storedFilenames.put(side, storageService.storeIdentityPhoto(file)));
        AccountProfileResponse profile = accountProfileService.updateIdentityPhotos(user.id().toString(), storedFilenames);
        return new IdentityPhotosResponse(profile.identityPhotoUrl(), profile.identityPhotoBackUrl());
    }

    @GetMapping("/identity-photo")
    public ResponseEntity<Resource> identityPhoto(HttpServletRequest request) {
        AuthenticatedUser user = currentUserService.requireUser(request);
        return privateImage(accountProfileService.identityPhoto(user.id().toString(), IdentityDocumentSide.FRONT));
    }

    @GetMapping("/identity-photo/back")
    public ResponseEntity<Resource> identityPhotoBack(HttpServletRequest request) {
        AuthenticatedUser user = currentUserService.requireUser(request);
        return privateImage(accountProfileService.identityPhoto(user.id().toString(), IdentityDocumentSide.BACK));
    }

    static ResponseEntity<Resource> privateImage(StorageService.StoredFile file) {
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    private String absoluteUrl(String relativeUrl) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
    }
}
