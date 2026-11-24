
package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;

    public AdminUserController(UserRepository userRepository,
                               SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "") String search,
            Model model) {

        String keyword = search.trim();

        List<User> users;

        if (keyword.isEmpty()) {
            users = userRepository.findAll();
        } else {
            users = userRepository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdAsc(
                            keyword, keyword, keyword
                    );
        }

        model.addAttribute("users", users);
        model.addAttribute("search", search);

        return "admin-users";
    }

    @GetMapping("/{id}")
    public String userDetails(@PathVariable Long id, Model model) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Naudotojas nerastas"
                ));

        model.addAttribute("user", user);

        return "admin-user-details";
    }

    @PostMapping("/{id}/block")
    public String blockUser(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                ));

        // Administratorius negali užblokuoti savo paskyros.
        if (user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Negalima užblokuoti savo paskyros"
            );
        }

        user.setActive(false);
        userRepository.save(user);

        expireSessions(user.getEmail());

        return "redirect:/admin/users";
    }

    /** Nutraukia visas užblokuoto naudotojo aktyvias sesijas. */
    private void expireSessions(String email) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails details
                    && details.getUsername().equalsIgnoreCase(email)) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }

    @PostMapping("/{id}/unblock")
    public String unblockUser(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                ));

        user.setActive(true);
        userRepository.save(user);

        return "redirect:/admin/users";
    }
}
