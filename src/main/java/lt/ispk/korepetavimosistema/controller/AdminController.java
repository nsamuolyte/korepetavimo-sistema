
package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.TutorProfile;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TutorProfileRepository tutorProfileRepository;

    public AdminController(TutorProfileRepository tutorProfileRepository) {
        this.tutorProfileRepository = tutorProfileRepository;
    }

    @GetMapping("/tutors")
    public String pendingTutors(Model model) {

        model.addAttribute(
                "tutors",
                tutorProfileRepository.findByApprovedFalseAndRejectedFalse()
        );

        return "admin-tutors";
    }

    @PostMapping("/tutors/{id}/approve")
    public String approveTutor(@PathVariable Long id) {

        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow();

        profile.setApproved(true);
        profile.setRejected(false);

        tutorProfileRepository.save(profile);

        return "redirect:/admin/tutors";
    }

    @PostMapping("/tutors/{id}/reject")
    public String rejectTutor(@PathVariable Long id) {

        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow();

        profile.setApproved(false);
        profile.setRejected(true);

        tutorProfileRepository.save(profile);

        return "redirect:/admin/tutors";
    }
}
