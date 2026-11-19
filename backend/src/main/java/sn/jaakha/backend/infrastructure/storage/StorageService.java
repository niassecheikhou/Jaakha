package sn.jaakha.backend.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);
    private static final String IDENTITY_FOLDER = "identity-documents";
    private static final Pattern STORED_FILENAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp)$");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );
    private static final Map<String, MediaType> MEDIA_TYPES_BY_EXTENSION = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "webp", MediaType.parseMediaType("image/webp")
    );

    private final Path uploadPath;
    private final Path privateUploadPath;

    public StorageService(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.private-upload.dir}") String privateUploadDir
    ) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.privateUploadPath = Paths.get(privateUploadDir).toAbsolutePath().normalize();
        if (privateUploadPath.startsWith(uploadPath) || uploadPath.startsWith(privateUploadPath)) {
            throw new IllegalStateException("app.private-upload.dir ne doit pas etre dans app.upload.dir (et inversement).");
        }
    }

    public String storeProviderPhoto(MultipartFile file) {
        return "/uploads/" + storeImage(file, uploadPath, "providers");
    }

    public String storeAccountPhoto(MultipartFile file) {
        return "/uploads/" + storeImage(file, uploadPath, "profiles");
    }

    /**
     * Stocke la piece d'identite hors du dossier public et retourne uniquement le nom du fichier.
     */
    public String storeIdentityPhoto(MultipartFile file) {
        String storedPath = storeImage(file, privateUploadPath, IDENTITY_FOLDER);
        return storedPath.substring(storedPath.lastIndexOf('/') + 1);
    }

    /**
     * Charge une piece d'identite a partir de la valeur enregistree en base
     * (nom du fichier, ou ancienne URL publique /uploads/identity-documents/...).
     */
    public StoredFile loadIdentityPhoto(String storedValue) {
        Path file = identityFile(storedValue)
                .filter(Files::isRegularFile)
                .orElseThrow(() -> new IllegalArgumentException("La piece d'identite est introuvable."));
        String filename = file.getFileName().toString();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        return new StoredFile(new PathResource(file), MEDIA_TYPES_BY_EXTENSION.get(extension));
    }

    /**
     * Supprime une piece d'identite remplacee (donnee personnelle : on ne garde pas d'anciennes copies).
     * Un echec est journalise sans bloquer la mise a jour du dossier.
     */
    public void deleteIdentityPhoto(String storedValue) {
        identityFile(storedValue).ifPresent(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException exception) {
                LOGGER.warn("Ancienne piece d'identite non supprimee : {}", file.getFileName(), exception);
            }
        });
    }

    /**
     * Chemin d'une piece d'identite dans le stockage prive, uniquement pour un nom de fichier genere
     * par le stockage (UUID + extension) : aucun chemin arbitraire ne peut etre resolu.
     */
    private Optional<Path> identityFile(String storedValue) {
        String filename = storedValue == null ? "" : storedValue.substring(storedValue.lastIndexOf('/') + 1);
        if (!STORED_FILENAME.matcher(filename).matches()) {
            return Optional.empty();
        }
        return Optional.of(privateUploadPath.resolve(IDENTITY_FOLDER).resolve(filename).normalize());
    }

    /**
     * Deplace les pieces d'identite deposees avant la securisation depuis le dossier public.
     */
    @PostConstruct
    void moveLegacyIdentityDocuments() {
        Path legacyFolder = uploadPath.resolve(IDENTITY_FOLDER);
        if (!Files.isDirectory(legacyFolder)) {
            return;
        }
        try {
            Path targetFolder = privateUploadPath.resolve(IDENTITY_FOLDER);
            Files.createDirectories(targetFolder);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyFolder)) {
                for (Path file : files) {
                    Files.move(file, targetFolder.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.delete(legacyFolder);
            LOGGER.info("Pieces d'identite deplacees vers le stockage prive {}", targetFolder);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de deplacer les pieces d'identite vers le stockage prive.", exception);
        }
    }

    private String storeImage(MultipartFile file, Path root, String folder) {
        validateImage(file);
        try {
            Path targetFolder = root.resolve(folder);
            Files.createDirectories(targetFolder);
            String contentType = file.getContentType();
            String filename = UUID.randomUUID() + EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
            Path target = targetFolder.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return folder + "/" + filename;
        } catch (IOException exception) {
            // Cause technique journalisee (droits du dossier, disque plein...) ; message generique pour l'utilisateur.
            LOGGER.error("Echec d'ecriture de la photo dans {}/{}", root, folder, exception);
            throw new IllegalArgumentException("La photo n'a pas pu etre enregistree. Merci de reessayer.");
        }
    }

    /**
     * Verifie qu'un fichier est une image acceptee (JPG, PNG, WEBP), sans l'enregistrer.
     */
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("L'image est vide ou absente.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("La photo doit etre une image JPG, PNG ou WEBP.");
        }
    }

    public record StoredFile(PathResource resource, MediaType mediaType) {
    }
}
