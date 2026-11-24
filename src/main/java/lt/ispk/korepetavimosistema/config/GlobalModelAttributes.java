package lt.ispk.korepetavimosistema.config;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.repository.MessageRepository;
import lt.ispk.korepetavimosistema.repository.NotificationRepository;
import lt.ispk.korepetavimosistema.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Prideda prisijungusį naudotoją ir neperskaitytų žinučių / pranešimų skaičių
 * į kiekvieno puslapio modelį – juos naudoja bendras meniu (fragments/navbar.html).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;

    public GlobalModelAttributes(UserRepository userRepository,
                                 MessageRepository messageRepository,
                                 NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
    }

    @ModelAttribute
    public void navigation(Authentication authentication, Model model) {
        User user = null;
        long unreadMessages = 0;
        long unreadNotifications = 0;

        if (authentication != null && authentication.isAuthenticated()) {
            user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                unreadMessages = messageRepository.countByRecipientIdAndReadFalse(user.getId());
                unreadNotifications = notificationRepository.countByRecipientIdAndReadFalse(user.getId());
            }
        }

        model.addAttribute("navUser", user);
        model.addAttribute("navUnreadMessages", unreadMessages);
        model.addAttribute("navUnreadNotifications", unreadNotifications);
    }
}
