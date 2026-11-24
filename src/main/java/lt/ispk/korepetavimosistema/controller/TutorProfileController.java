
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

        String error = validateProfile(subject, educationLevel, hourlyRate,
                description, experienceYears);

        if (error != null) {

            model.addAttribute("error", error);
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

    /** Profilio duomenų patikra (išsamesnė validacija – likutis iš 1 sprinto). */
    private static String validateProfile(String subject, String educationLevel,
                                          BigDecimal hourlyRate, String description,
                                          int experienceYears) {
        if (subject == null || subject.isBlank() || subject.trim().length() > 100) {
            return "Mokomas dalykas privalomas (iki 100 simbolių).";
        }
        if (educationLevel == null || educationLevel.isBlank() || educationLevel.trim().length() > 100) {
            return "Mokymo lygis privalomas (iki 100 simbolių).";
        }
        if (hourlyRate == null
                || hourlyRate.compareTo(BigDecimal.ONE) < 0
                || hourlyRate.compareTo(new BigDecimal("500")) > 0) {
            return "Valandinė kaina turi būti nuo 1,00 iki 500,00 €.";
        }
        if (hourlyRate.scale() > 2) {
            return "Kaina gali turėti ne daugiau kaip du skaitmenis po kablelio.";
        }
        if (experienceYears < 0 || experienceYears > 60) {
            return "Patirtis turi būti nuo 0 iki 60 metų.";
        }
        if (description != null && description.length() > 2000) {
            return "Aprašymas negali būti ilgesnis nei 2000 simbolių.";
        }
        return null;
    }
}
