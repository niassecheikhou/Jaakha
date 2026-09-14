package sn.jaakha.backend.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
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

    private final Path uploadPath;

    public StorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeProviderPhoto(MultipartFile file) {
        return storeImage(file, "providers");
    }

    public String storeAccountPhoto(MultipartFile file) {
        return storeImage(file, "profiles");
    }

    public String storeIdentityPhoto(MultipartFile file) {
        return storeImage(file, "identity-documents");
    }

    private String storeImage(MultipartFile file, String folder) {
        validateImage(file);
        try {
            Path targetFolder = uploadPath.resolve(folder);
            Files.createDirectories(targetFolder);
            String contentType = file.getContentType();
            String filename = UUID.randomUUID() + EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
            Path target = targetFolder.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException exception) {
            throw new IllegalArgumentException("La photo n'a pas pu etre enregistree. Merci de reessayer.");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("La photo du profil est obligatoire.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("La photo doit etre une image JPG, PNG ou WEBP.");
        }
    }
}
