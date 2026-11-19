package sn.jaakha.backend.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.repository.AppUserRepository;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String phone;
    private final String password;

    public AdminBootstrap(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap-email}") String email,
            @Value("${app.admin.bootstrap-phone}") String phone,
            @Value("${app.admin.bootstrap-password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email == null || email.isBlank() || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        AppUser admin = new AppUser(
                "Admin Jaakha",
                email.trim().toLowerCase(java.util.Locale.ROOT),
                phone,
                passwordEncoder.encode(password),
                Role.ADMIN
        );
        admin.setEmailVerified(true);
        userRepository.save(admin);
    }
}
