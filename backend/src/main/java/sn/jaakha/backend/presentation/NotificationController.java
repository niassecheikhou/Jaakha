package sn.jaakha.backend.presentation;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.NotificationService;
import sn.jaakha.backend.application.dto.NotificationResponse;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<NotificationResponse> list(HttpServletRequest request) {
        return notificationService.listForUser(currentUserService.requireUser(request).id());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(HttpServletRequest request, @PathVariable UUID id) {
        return notificationService.markRead(id, currentUserService.requireUser(request).id());
    }
}
