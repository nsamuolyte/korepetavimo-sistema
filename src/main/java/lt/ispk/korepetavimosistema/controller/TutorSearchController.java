
package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
            Model model) {

        var tutor = tutorRepository.findPublicById(id)
                .orElseThrow(() ->
                        new org.springframework.web.server
                                .ResponseStatusException(
                                org.springframework.http
                                        .HttpStatus.NOT_FOUND,
                                "Korepetitorius nerastas"
                        ));

        model.addAttribute("tutor", tutor);

        return "tutor-details";
    }
}
