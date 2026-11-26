package sn.jaakha.backend.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sn.jaakha.backend.application.dto.AccountProfileRequest;
import sn.jaakha.backend.application.dto.AccountProfileResponse;
import sn.jaakha.backend.domain.enums.IdentityDocumentSide;
import sn.jaakha.backend.domain.model.AccountProfile;
import sn.jaakha.backend.domain.repository.AccountProfileRepository;
import sn.jaakha.backend.infrastructure.storage.StorageService;

@Service
public class AccountProfileService {
    private final AccountProfileRepository accountProfileRepository;
    private final StorageService storageService;

    public AccountProfileService(AccountProfileRepository accountProfileRepository, StorageService storageService) {
        this.accountProfileRepository = accountProfileRepository;
        this.storageService = storageService;
    }

    @Transactional
    public AccountProfileResponse getMine(String ownerKey) {
        return AccountProfileResponse.from(getOrCreate(ownerKey));
    }

    @Transactional
    public AccountProfileResponse updatePhoto(String ownerKey, String photoUrl) {
        AccountProfile profile = getOrCreate(ownerKey);
        profile.setPhotoUrl(photoUrl);
        return AccountProfileResponse.from(accountProfileRepository.save(profile));
    }

    /**
     * Enregistre une ou deux faces de la piece d'identite (noms de fichiers deja stockes en prive).
     * Les fichiers remplaces sont supprimes du disque.
     */
    @Transactional
    public AccountProfileResponse updateIdentityPhotos(String ownerKey, Map<IdentityDocumentSide, String> storedFilenames) {
        AccountProfile profile = getOrCreate(ownerKey);
        List<String> replacedFiles = new ArrayList<>();
        storedFilenames.forEach((side, filename) -> {
            String previous = profile.getIdentityPhoto(side);
            if (previous != null && !previous.equals(filename)) {
                replacedFiles.add(previous);
            }
            profile.setIdentityPhoto(side, filename);
        });
        AccountProfileResponse response = AccountProfileResponse.from(accountProfileRepository.save(profile));
        deleteAfterCommit(replacedFiles);
        return response;
    }

    /**
     * Les anciens fichiers ne sont supprimes qu'une fois la base a jour : si la transaction echoue,
     * le dossier garde ses pieces precedentes intactes.
     */
    private void deleteAfterCommit(List<String> files) {
        if (files.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            files.forEach(storageService::deleteIdentityPhoto);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                files.forEach(storageService::deleteIdentityPhoto);
            }
        });
    }

    @Transactional(readOnly = true)
    public StorageService.StoredFile identityPhoto(String ownerKey, IdentityDocumentSide side) {
        String storedValue = accountProfileRepository.findByOwnerKey(ownerKey)
                .map(profile -> profile.getIdentityPhoto(side))
                .orElseThrow(() -> new IllegalArgumentException("Le " + side.label() + " de la piece d'identite est introuvable."));
        return storageService.loadIdentityPhoto(storedValue);
    }

    @Transactional
    public AccountProfileResponse saveMine(String ownerKey, AccountProfileRequest request) {
        // photoUrl n'est pas modifiable ici : seul l'upload (/api/account/me/photo) le renseigne.
        AccountProfile profile = getOrCreate(ownerKey);
        profile.setBio(blankToNull(request.bio()));
        profile.setZone(blankToNull(request.zone()));
        profile.setMonthlyRate(request.monthlyRate());
        profile.setExperienceYears(request.experienceYears());
        profile.setSkills(joinSkills(request.skills()));
        profile.setGuardianFullName(blankToNull(request.guardianFullName()));
        profile.setGuardianPhone(blankToNull(request.guardianPhone()));
        if (request.address() != null) {
            profile.setAddress(blankToNull(request.address()));
        }
        return AccountProfileResponse.from(accountProfileRepository.save(profile));
    }

    private AccountProfile getOrCreate(String ownerKey) {
        return accountProfileRepository.findByOwnerKey(ownerKey)
                .orElseGet(() -> accountProfileRepository.save(new AccountProfile(ownerKey)));
    }

    private String joinSkills(Iterable<String> skills) {
        if (skills == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(skill.trim());
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
