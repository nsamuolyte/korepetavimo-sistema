package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.Lesson;
import lt.ispk.korepetavimosistema.model.LessonStatus;
import lt.ispk.korepetavimosistema.model.TutorProfile;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.model.UserRole;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import lt.ispk.korepetavimosistema.service.AvailabilityService;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.LessonService;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pamokų tvarkaraštis, rezervavimas, perkėlimas ir atšaukimas
 * (US-M03, US-M04, US-T03, US-T04, VIR-F4).
 */
@Controller
@RequestMapping("/lessons")
public class LessonController {

    private final LessonService lessonService;
    private final AvailabilityService availabilityService;
    private final TutorProfileRepository tutorProfileRepository;
    private final CurrentUserService currentUserService;

    public LessonController(LessonService lessonService,
                            AvailabilityService availabilityService,
                            TutorProfileRepository tutorProfileRepository,
                            CurrentUserService currentUserService) {
        this.lessonService = lessonService;
        this.availabilityService = availabilityService;
        this.tutorProfileRepository = tutorProfileRepository;
        this.currentUserService = currentUserService;
    }

    // ---------- Tvarkaraštis ----------

    @GetMapping
    public String schedule(Authentication authentication, Model model) {
        User user = currentUserService.get(authentication);
        LocalDateTime now = LocalDateTime.now();

        List<Lesson> upcoming = new ArrayList<>();
        List<Lesson> history = new ArrayList<>();
        List<Long> inProgressIds = new ArrayList<>();

        for (Lesson l : lessonService.lessonsFor(user)) {
            if (l.getStatus() == LessonStatus.SCHEDULED && l.getEndTime().isAfter(now)) {
                upcoming.add(l);
                if (!l.getStartTime().isAfter(now)) {
                    inProgressIds.add(l.getId());
                }
            } else {
                history.add(l);
            }
        }
        history.sort(Comparator.comparing(Lesson::getStartTime).reversed());

        model.addAttribute("upcoming", upcoming);
        model.addAttribute("history", history);
        model.addAttribute("inProgressIds", inProgressIds);
        model.addAttribute("me", user);
        return "lessons";
    }

    // ---------- Rezervavimas ----------

    @GetMapping("/book/{profileId}")
    public String bookForm(Authentication authentication,
                           @PathVariable Long profileId,
                           @RequestParam(defaultValue = "60") int minutes,
                           Model model) {
        User user = currentUserService.get(authentication);
        TutorProfile profile = publicProfile(profileId);

        if (!AvailabilityService.DURATIONS.contains(minutes)) {
            minutes = 60;
        }

        model.addAttribute("profile", profile);
        model.addAttribute("minutes", minutes);
        model.addAttribute("durations", AvailabilityService.DURATIONS);
        model.addAttribute("price", LessonService.calculatePrice(profile.getHourlyRate(), minutes));
        model.addAttribute("slots", availabilityService.freeSlots(profile.getUser().getId(), minutes, null));
        model.addAttribute("daysAhead", AvailabilityService.BOOKING_DAYS_AHEAD);
        model.addAttribute("isGuardian", user.getRole() == UserRole.GUARDIAN);
        model.addAttribute("children",
                user.getRole() == UserRole.GUARDIAN ? lessonService.approvedChildren(user) : List.of());
        return "book-lesson";
    }

    @PostMapping("/book/{profileId}")
    public String book(Authentication authentication,
                       @PathVariable Long profileId,
                       @RequestParam(required = false) String start,
                       @RequestParam int minutes,
                       @RequestParam(required = false) Long studentId,
                       @RequestParam(required = false) String subject,
                       RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        TutorProfile profile = publicProfile(profileId);

        try {
            Lesson lesson = lessonService.book(user, profile.getUser().getId(), studentId,
                    parse(start), minutes, subject);
            redirect.addFlashAttribute("success", "Pamoka rezervuota: "
                    + LtFormat.dateTime(lesson.getStartTime()) + ", "
                    + lesson.getSubject() + ".");
            return "redirect:/lessons";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/lessons/book/" + profileId + "?minutes=" + minutes;
        }
    }

    // ---------- Perkėlimas ----------

    @GetMapping("/{id}/reschedule")
    public String rescheduleForm(Authentication authentication,
                                 @PathVariable Long id,
                                 Model model,
                                 RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            Lesson lesson = lessonService.getForUser(user, id);
            if (lesson.getStatus() != LessonStatus.SCHEDULED) {
                throw new BookingException("Perkelti galima tik suplanuotą pamoką.");
            }
            int minutes = (int) Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes();
            model.addAttribute("lesson", lesson);
            model.addAttribute("minutes", minutes);
            model.addAttribute("slots",
                    availabilityService.freeSlots(lesson.getTutor().getId(), minutes, lesson.getId()));
            return "reschedule-lesson";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/lessons";
        }
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(Authentication authentication,
                             @PathVariable Long id,
                             @RequestParam(required = false) String start,
                             RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            Lesson lesson = lessonService.reschedule(user, id, parse(start));
            redirect.addFlashAttribute("success", "Pamoka perkelta į "
                    + LtFormat.dateTime(lesson.getStartTime()) + ".");
            return "redirect:/lessons";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/lessons/" + id + "/reschedule";
        }
    }

    // ---------- Atšaukimas ir užbaigimas ----------

    @PostMapping("/{id}/cancel")
    public String cancel(Authentication authentication,
                         @PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            lessonService.cancel(user, id, reason);
            redirect.addFlashAttribute("success", "Pamoka atšaukta.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lessons";
    }

    @PostMapping("/{id}/complete")
    public String complete(Authentication authentication,
                           @PathVariable Long id,
                           RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            lessonService.complete(user, id);
            redirect.addFlashAttribute("success", "Pamoka pažymėta kaip įvykusi.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lessons";
    }

    // ---------- Pagalbiniai ----------

    private TutorProfile publicProfile(Long profileId) {
        return tutorProfileRepository.findPublicById(profileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Korepetitorius nerastas"));
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BookingException("Pasirinkite pamokos laiką.");
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new BookingException("Neteisingas laiko formatas.");
        }
    }
}
