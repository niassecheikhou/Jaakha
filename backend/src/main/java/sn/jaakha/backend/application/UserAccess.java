package sn.jaakha.backend.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.model.AppUser;
import sn.jaakha.backend.domain.repository.AppUserRepository;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@Component
public class UserAccess {
    private final AppUserRepository userRepository;

    public UserAccess(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser requireUser(JaakhaUserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        }
        return userRepository.findById(principal.id())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable ou desactive."));
    }

    public AppUser requireRole(JaakhaUserPrincipal principal, Role role) {
        AppUser user = requireUser(principal);
        if (user.getRole() != role) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Role insuffisant.");
        }
        return user;
    }

    public void requireSameUserOrAdmin(AppUser currentUser, UUID targetUserId) {
        if (currentUser.getRole() != Role.ADMIN && !currentUser.getId().equals(targetUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
    }
}
