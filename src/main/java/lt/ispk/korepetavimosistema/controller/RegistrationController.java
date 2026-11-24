package lt.ispk.korepetavimosistema.controller;

import jakarta.validation.Valid;
import lt.ispk.korepetavimosistema.dto.RegistrationForm;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.model.UserRole;
import lt.ispk.korepetavimosistema.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
    public String registrationPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult bindingResult,
            Model model) {

        // Formos lygio taisyklės (Bean Validation).
        if (bindingResult.hasErrors()) {
            model.addAttribute("error",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());
            form.setPassword(null);
            return "register";
        }

        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);

        if (form.getRole() == UserRole.ADMIN) {
            model.addAttribute("error",
                    "Administratoriaus paskyros registruoti negalima.");
            form.setPassword(null);
            return "register";
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            model.addAttribute("error",
                    "Šis el. pašto adresas jau naudojamas.");
            form.setPassword(null);
            return "register";
        }

        User user = new User();
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setActive(true);

        userRepository.save(user);

        return "redirect:/login?registered";
    }
}
