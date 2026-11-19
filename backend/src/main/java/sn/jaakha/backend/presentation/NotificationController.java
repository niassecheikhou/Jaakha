package sn.jaakha.backend.presentation;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.jaakha.backend.application.NotificationService;
import sn.jaakha.backend.application.UserAccess;
import sn.jaakha.backend.application.dto.NotificationResponse;


@RestController
@RequestMapping("/api/notifications")
    private final NotificationService notificationService;

    private final UserAccess userAccess;

    public NotificationController(NotificationService notificationService, UserAccess userAccess) {
        this.notificationService = notificationService;
        this.userAccess = userAccess;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal JaakhaUserPrincipal principal) {
        return notificationService.listFor(userAccess.requireUser(principal));

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(HttpServletRequest request, @PathVariable UUID id) {
        return notificationService.markRead(id, currentUserService.requireUser(request).id());

    }
}
