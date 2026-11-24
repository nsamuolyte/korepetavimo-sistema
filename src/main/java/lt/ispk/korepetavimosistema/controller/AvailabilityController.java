package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.service.AvailabilityService;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Korepetitoriaus darbo laiko valdymas (US-K03, FR-15, VIR-I4).
 */
@Controller
@RequestMapping("/tutor/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final CurrentUserService currentUserService;

    public AvailabilityController(AvailabilityService availabilityService,
                                  CurrentUserService currentUserService) {
        this.availabilityService = availabilityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        User tutor = currentUserService.get(authentication);
        model.addAttribute("availabilities", availabilityService.forTutor(tutor.getId()));
        model.addAttribute("days", DayOfWeek.values());
        return "tutor-availability";
    }

    @PostMapping
    public String add(Authentication authentication,
                      @RequestParam DayOfWeek dayOfWeek,
                      @RequestParam String startTime,
                      @RequestParam String endTime,
                      RedirectAttributes redirect) {
        User tutor = currentUserService.get(authentication);
        try {
            availabilityService.add(tutor, dayOfWeek,
                    LocalTime.parse(startTime), LocalTime.parse(endTime));
            redirect.addFlashAttribute("success", "Darbo laikas pridėtas.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DateTimeParseException e) {
            redirect.addFlashAttribute("error", "Neteisingas laiko formatas. Naudokite HH:mm.");
        }
        return "redirect:/tutor/availability";
    }

    @PostMapping("/{id}/delete")
    public String delete(Authentication authentication,
                         @PathVariable Long id,
                         RedirectAttributes redirect) {
        User tutor = currentUserService.get(authentication);
        try {
            availabilityService.delete(tutor, id);
            redirect.addFlashAttribute("success", "Darbo laikas pašalintas.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tutor/availability";
    }
}
