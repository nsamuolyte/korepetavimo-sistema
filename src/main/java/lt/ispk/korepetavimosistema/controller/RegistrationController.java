
package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.model.UserRole;
import lt.ispk.korepetavimosistema.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String registrationPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam UserRole role,
            Model model) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (firstName.isBlank() || lastName.isBlank()
                || normalizedEmail.isBlank()
                || !normalizedEmail.contains("@")
                || password.length() < 8) {
            model.addAttribute("error",
                    "Patikrinkite įvestus duomenis. Slaptažodis turi būti bent 8 simbolių.");
            return "register";
        }

        if (role == UserRole.ADMIN) {
            model.addAttribute("error",
                    "Administratoriaus paskyros registruoti negalima.");
            return "register";
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            model.addAttribute("error",
                    "Šis el. pašto adresas jau naudojamas.");
            return "register";
        }

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);

        return "redirect:/login?registered";
    }
}
