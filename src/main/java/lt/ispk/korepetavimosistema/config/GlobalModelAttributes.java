package lt.ispk.korepetavimosistema.config;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Prideda prisijungusį naudotoją į kiekvieno puslapio modelį,
 * kad bendras viršutinis meniu (fragments/navbar.html) žinotų, ką rodyti.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("navUser")
    public User navUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
