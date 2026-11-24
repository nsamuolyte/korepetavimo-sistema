package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.Lesson;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import lt.ispk.korepetavimosistema.service.AvailabilityService;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.LessonService;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Korepetitorius pats sukuria pamoką savo mokiniui (US-K04, FR-21).
 */
@Controller
@RequestMapping("/tutor/lessons")
public class TutorLessonController {

    private final LessonService lessonService;
    private final AvailabilityService availabilityService;
    private final CurrentUserService currentUserService;
    private final TutorProfileRepository tutorProfileRepository;

    public TutorLessonController(
            LessonService lessonService,
            AvailabilityService availabilityService,
            CurrentUserService currentUserService,
            TutorProfileRepository tutorProfileRepository) {

        this.lessonService = lessonService;
        this.availabilityService = availabilityService;
        this.currentUserService = currentUserService;
        this.tutorProfileRepository = tutorProfileRepository;
    }

    @GetMapping("/new")
    public String form(Authentication authentication, Model model) {

        User tutor = currentUserService.get(authentication);

        model.addAttribute("students",
                lessonService.tutorStudents(tutor));

        model.addAttribute("subjects",
                tutorProfileRepository.findByUserId(tutor.getId())
                        .map(profile -> profile.getSubjects())
                        .orElse(Set.of()));

        model.addAttribute("durations", AvailabilityService.DURATIONS);

        model.addAttribute("availabilities",
                availabilityService.forTutor(tutor.getId()));

        model.addAttribute("minDate", LocalDate.now().plusDays(1));

        model.addAttribute("maxDate",
                LocalDate.now().plusDays(
                        AvailabilityService.BOOKING_DAYS_AHEAD));

        return "tutor-lesson-new";
    }

    @PostMapping("/new")
    public String create(
            Authentication authentication,
            @RequestParam(required = false) Long studentId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam int minutes,
            @RequestParam(required = false) String subject,
            RedirectAttributes redirect) {

        User tutor = currentUserService.get(authentication);

        try {
            Lesson lesson = lessonService.book(
                    tutor,
                    tutor.getId(),
                    studentId,
                    LocalDate.parse(date).atTime(LocalTime.parse(time)),
                    minutes,
                    subject);

            redirect.addFlashAttribute("success", "Pamoka sukurta: "
                    + LtFormat.dateTime(lesson.getStartTime()) + ", mokinys "
                    + LessonService.fullName(lesson.getStudent()) + ".");

            return "redirect:/lessons";

        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());

        } catch (DateTimeParseException e) {
            redirect.addFlashAttribute("error",
                    "Neteisinga data arba laikas.");
        }

        return "redirect:/tutor/lessons/new";
    }
}