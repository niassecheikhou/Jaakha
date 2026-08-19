package sn.jaakha.backend.application;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.AdminCreateUserRequest;
import sn.jaakha.backend.application.dto.UserResponse;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.repository.AppUserRepository;

@Service
public class AdminService {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final AppUserRepository userRepository;
    private final AuthService authService;
    private final SecureRandom random = new SecureRandom();

    public AdminService(AppUserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        String password = request.password() == null || request.password().isBlank()
                ? generatePassword()
                : request.password();
        AppUser user = authService.createUserByAdmin(
                request.fullName(),
                request.email(),
                request.phone(),
                password,
                request.role()
        );
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder();
        for (int index = 0; index < 12; index++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
