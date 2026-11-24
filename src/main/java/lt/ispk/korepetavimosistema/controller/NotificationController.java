package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.Notification;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.repository.NotificationRepository;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Pranešimų sąrašas (US-K09, FR-29, VIR-P3).
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService,
                                  NotificationRepository notificationRepository,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        User me = currentUserService.get(authentication);
        model.addAttribute("notifications", notificationService.forUser(me));
        return "notifications";
    }

    /** Atidaro pranešimą: pažymi perskaitytu ir nukreipia į susijusį puslapį. */
    @GetMapping("/{id}/open")
    public String open(Authentication authentication, @PathVariable Long id) {
        User me = currentUserService.get(authentication);
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n == null || !n.getRecipient().getId().equals(me.getId())) {
            return "redirect:/notifications";
        }
        n.setRead(true);
        notificationRepository.save(n);
        String link = n.getLink();
        // Leidžiamos tik vidinės nuorodos.
        return (link != null && link.startsWith("/") && !link.startsWith("//"))
                ? "redirect:" + link
                : "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String readAll(Authentication authentication) {
        User me = currentUserService.get(authentication);
        notificationService.markAllRead(me);
        return "redirect:/notifications";
    }
}
