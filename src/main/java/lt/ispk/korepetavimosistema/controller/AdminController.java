package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.NotificationType;
import lt.ispk.korepetavimosistema.model.TutorProfile;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import lt.ispk.korepetavimosistema.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TutorProfileRepository tutorProfileRepository;
    private final NotificationService notificationService;

    public AdminController(TutorProfileRepository tutorProfileRepository,
                           NotificationService notificationService) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/tutors")
    public String pendingTutors(Model model) {
        model.addAttribute("tutors",
                tutorProfileRepository.findByApprovedFalseAndRejectedFalse());
        return "admin-tutors";
    }

    @PostMapping("/tutors/{id}/approve")
    @Transactional
    public String approveTutor(@PathVariable Long id) {
        TutorProfile profile = findProfile(id);

        profile.setApproved(true);
        profile.setRejected(false);
        tutorProfileRepository.save(profile);

        notificationService.notify(profile.getUser(), NotificationType.TUTOR_PROFILE,
                "Jūsų korepetitoriaus profilis patvirtintas. Jis jau matomas paieškoje.",
                "/tutor/profile");

        return "redirect:/admin/tutors?approved";
    }

    @PostMapping("/tutors/{id}/reject")
    @Transactional
    public String rejectTutor(@PathVariable Long id) {
        TutorProfile profile = findProfile(id);

        profile.setApproved(false);
        profile.setRejected(true);
        tutorProfileRepository.save(profile);

        notificationService.notify(profile.getUser(), NotificationType.TUTOR_PROFILE,
                "Jūsų korepetitoriaus profilis atmestas. Pataisykite informaciją ir išsaugokite profilį iš naujo.",
                "/tutor/profile");

        return "redirect:/admin/tutors?rejected";
    }

    private TutorProfile findProfile(Long id) {
        return tutorProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}