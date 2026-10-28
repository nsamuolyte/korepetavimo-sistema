
package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.TutorProfile;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import lt.ispk.korepetavimosistema.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/tutor")
public class TutorProfileController {

    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;

    public TutorProfileController(
            TutorProfileRepository tutorProfileRepository,
            UserRepository userRepository) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        var profile = tutorProfileRepository.findByUserId(user.getId())
                .orElseGet(TutorProfile::new);

        model.addAttribute("profile", profile);
        return "tutor-profile";
    }

    @PostMapping("/profile")
    public String saveProfile(
            Authentication authentication,
            @RequestParam String subject,
            @RequestParam String educationLevel,
            @RequestParam BigDecimal hourlyRate,
            @RequestParam String description,
            @RequestParam int experienceYears,
            Model model) {

        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        if (subject.isBlank()
                || educationLevel.isBlank()
                || hourlyRate.compareTo(BigDecimal.ZERO) < 0
                || experienceYears < 0
                || description.length() > 2000) {

            model.addAttribute("error",
                    "Patikrinkite įvestus profilio duomenis.");
            model.addAttribute("profile",
                    tutorProfileRepository.findByUserId(user.getId())
                            .orElseGet(TutorProfile::new));

            return "tutor-profile";
        }

        var profile = tutorProfileRepository.findByUserId(user.getId())
                .orElseGet(TutorProfile::new);

        profile.setUser(user);
        profile.setSubject(subject.trim());
        profile.setEducationLevel(educationLevel.trim());
        profile.setHourlyRate(hourlyRate);
        profile.setDescription(description.trim());
        profile.setExperienceYears(experienceYears);

        // Redagavus profilį, jį reikės patvirtinti iš naujo.
        profile.setApproved(false);
        profile.setRejected(false);

        tutorProfileRepository.save(profile);

        return "redirect:/tutor/profile?saved";
    }
}
