
package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Controller
public class GuardianStudentController {

    private final UserRepository userRepository;
    private final GuardianStudentRepository linkRepository;

    public GuardianStudentController(
            UserRepository userRepository,
            GuardianStudentRepository linkRepository) {
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
    }

    @GetMapping("/guardian/students")
    public String guardianStudents(
            Authentication authentication,
            Model model) {

        User guardian = currentUser(authentication);

        model.addAttribute(
                "links",
                linkRepository.findByGuardianId(guardian.getId())
        );

        return "guardian-students";
    }

    @PostMapping("/guardian/students")
    public String requestLink(
            Authentication authentication,
            @RequestParam String studentEmail) {

        User guardian = currentUser(authentication);

        String email = studentEmail.trim()
                .toLowerCase(Locale.ROOT);

        userRepository.findByEmail(email).ifPresent(student -> {

            if (student.getRole() != UserRole.STUDENT
                    || !student.isActive()) {
                return;
            }

            var existing = linkRepository
                    .findByGuardianIdAndStudentId(
                            guardian.getId(),
                            student.getId()
                    );

            if (existing.isEmpty()) {
                GuardianStudent link = new GuardianStudent();
                link.setGuardian(guardian);
                link.setStudent(student);
                link.setStatus(LinkStatus.PENDING);

                linkRepository.save(link);
            } else if (existing.get().getStatus()
                    == LinkStatus.REJECTED) {

                GuardianStudent link = existing.get();
                link.setStatus(LinkStatus.PENDING);
                linkRepository.save(link);
            }
        });

        // Neatskleidžiame, ar konkretus el. paštas
        // egzistuoja sistemoje.
        return "redirect:/guardian/students?requested";
    }

    @GetMapping("/student/guardians")
    public String studentGuardians(
            Authentication authentication,
            Model model) {

        User student = currentUser(authentication);

        model.addAttribute(
                "links",
                linkRepository.findByStudentId(student.getId())
        );

        return "student-guardians";
    }

    @PostMapping("/student/guardians/{id}/approve")
    public String approve(
            @PathVariable Long id,
            Authentication authentication) {

        updateStatus(id, authentication, LinkStatus.APPROVED);

        return "redirect:/student/guardians";
    }

    @PostMapping("/student/guardians/{id}/reject")
    public String reject(
            @PathVariable Long id,
            Authentication authentication) {

        updateStatus(id, authentication, LinkStatus.REJECTED);

        return "redirect:/student/guardians";
    }

    private void updateStatus(
            Long id,
            Authentication authentication,
            LinkStatus newStatus) {

        User student = currentUser(authentication);

        GuardianStudent link = linkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                ));

        if (!link.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (link.getStatus() != LinkStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Prašymas jau apdorotas"
            );
        }

        if (!link.getGuardian().isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Globėjo paskyra neaktyvi"
            );
        }

        link.setStatus(newStatus);
        linkRepository.save(link);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow();
    }
}
