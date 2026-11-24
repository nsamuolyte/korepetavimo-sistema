package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Controller
@RequestMapping("/tutors")
public class TutorSearchController {

    private final TutorProfileRepository tutorRepository;

    public TutorSearchController(
            TutorProfileRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    @GetMapping
    public String listTutors(
            @RequestParam(defaultValue = "") String subject,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        BigDecimal priceLimit = maxPrice == null
                ? new BigDecimal("99999999")
                : maxPrice;

        model.addAttribute("tutors",
                tutorRepository.searchApprovedTutors(
                        subject.trim(), priceLimit));

        model.addAttribute("subject", subject);
        model.addAttribute("maxPrice", maxPrice);

        return "tutors";
    }

    @GetMapping("/{id}")
    public String tutorDetails(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        var tutor = tutorRepository.findPublicById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Korepetitorius nerastas"
                ));

        boolean loggedIn = authentication != null && authentication.isAuthenticated();
        boolean canBook = loggedIn && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")
                        || a.getAuthority().equals("ROLE_GUARDIAN"));

        model.addAttribute("tutor", tutor);
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("canBook", canBook);

        return "tutor-details";
    }
}
