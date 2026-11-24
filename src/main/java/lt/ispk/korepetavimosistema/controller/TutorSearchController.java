package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.TutorProfile;
import lt.ispk.korepetavimosistema.repository.LessonPackageRepository;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import lt.ispk.korepetavimosistema.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/tutors")
public class TutorSearchController {

    private final TutorProfileRepository tutorRepository;
    private final LessonPackageRepository lessonPackageRepository;
    private final AvailabilityService availabilityService;

    public TutorSearchController(
            TutorProfileRepository tutorRepository,
            LessonPackageRepository lessonPackageRepository,
            AvailabilityService availabilityService) {
        this.tutorRepository = tutorRepository;
        this.lessonPackageRepository = lessonPackageRepository;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public String listTutors(
            @RequestParam(defaultValue = "") String subject,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        BigDecimal priceLimit = maxPrice == null
                ? new BigDecimal("99999999")
                : maxPrice;

        List<TutorProfile> tutors = tutorRepository.searchApprovedTutors(
                subject.trim(), priceLimit);

        LocalDate today = LocalDate.now();
        LocalDate lastDay = today.plusDays(AvailabilityService.BOOKING_DAYS_AHEAD);

        // Filtravimas pagal laisvą laiką (likutis iš 1 sprinto, FR-10).
        if (date != null) {
            if (date.isBefore(today) || date.isAfter(lastDay)) {
                model.addAttribute("dateError", "Pasirinkite datą nuo " + today + " iki " + lastDay + ".");
            } else {
                tutors = tutors.stream()
                        .filter(t -> availabilityService.hasFreeSlotOn(t.getUser().getId(), date))
                        .toList();
            }
        }

        model.addAttribute("tutors", tutors);
        model.addAttribute("subject", subject);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("date", date);
        model.addAttribute("minDate", today);
        model.addAttribute("maxDate", lastDay);

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
        model.addAttribute("packages",
                lessonPackageRepository.findByTutorIdAndActiveTrueOrderByPriceAsc(tutor.getUser().getId()));
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("canBook", canBook);

        return "tutor-details";
    }
}
