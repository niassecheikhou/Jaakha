package sn.jaakha.backend.presentation;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.NotificationService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.NotificationResponse;
import sn.jaakha.backend.infrastructure.security.JaakhaUserPrincipal;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserAccess userAccess;

    public NotificationController(NotificationService notificationService, UserAccess userAccess) {
        this.notificationService = notificationService;
        this.userAccess = userAccess;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal JaakhaUserPrincipal principal) {
        return notificationService.listFor(userAccess.requireUser(principal));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@AuthenticationPrincipal JaakhaUserPrincipal principal, @PathVariable UUID id) {
        return notificationService.markAsRead(userAccess.requireUser(principal), id);
    }
}
