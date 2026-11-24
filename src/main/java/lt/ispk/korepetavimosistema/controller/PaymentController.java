package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.LessonPackage;
import lt.ispk.korepetavimosistema.model.Payment;
import lt.ispk.korepetavimosistema.model.PaymentStatus;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.model.UserRole;
import lt.ispk.korepetavimosistema.repository.TutorProfileRepository;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.LessonService;
import lt.ispk.korepetavimosistema.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Pamokų paketų pirkimas, mokėjimų sistemos imitacija ir mokėjimų istorija
 * (US-M09, US-T08, FR-23–FR-26, VIR-F5).
 */
@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final LessonService lessonService;
    private final TutorProfileRepository tutorProfileRepository;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService,
                             LessonService lessonService,
                             TutorProfileRepository tutorProfileRepository,
                             CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.lessonService = lessonService;
        this.tutorProfileRepository = tutorProfileRepository;
        this.currentUserService = currentUserService;
    }

    /** Mokėjimų istorija ir įsigyti paketai. */
    @GetMapping
    public String history(Authentication authentication, Model model) {
        User user = currentUserService.get(authentication);
        model.addAttribute("payments", paymentService.paymentHistory(user));
        model.addAttribute("purchases", paymentService.purchases(user));
        return "payments";
    }

    /** Patvirtinimo langas: galutinė suma ir sąlygos (NFR-J3). */
    @GetMapping("/checkout/{packageId}")
    public String checkoutForm(Authentication authentication,
                               @PathVariable Long packageId,
                               Model model,
                               RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            LessonPackage pkg = paymentService.getActivePackage(packageId);
            model.addAttribute("pkg", pkg);
            model.addAttribute("profile",
                    tutorProfileRepository.findByUserId(pkg.getTutor().getId()).orElse(null));
            model.addAttribute("isGuardian", user.getRole() == UserRole.GUARDIAN);
            model.addAttribute("children", user.getRole() == UserRole.GUARDIAN
                    ? lessonService.approvedChildren(user) : List.of());
            return "checkout";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/payments";
        }
    }

    @PostMapping("/checkout/{packageId}")
    public String checkout(Authentication authentication,
                           @PathVariable Long packageId,
                           @RequestParam(required = false) Long studentId,
                           @RequestParam(defaultValue = "false") boolean acceptTerms,
                           RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        if (!acceptTerms) {
            redirect.addFlashAttribute("error", "Norėdami tęsti, sutikite su apmokėjimo ir atšaukimo sąlygomis.");
            return "redirect:/payments/checkout/" + packageId;
        }
        try {
            Payment payment = paymentService.startCheckout(user, packageId, studentId);
            return "redirect:/payments/" + payment.getId() + "/gateway";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/payments/checkout/" + packageId;
        }
    }

    /** Išorinės mokėjimų sistemos imitacijos langas. */
    @GetMapping("/{id}/gateway")
    public String gateway(Authentication authentication,
                          @PathVariable Long id,
                          Model model,
                          RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            Payment payment = paymentService.getForPayer(user, id);
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new BookingException("Šis mokėjimas jau apdorotas.");
            }
            model.addAttribute("payment", payment);
            return "payment-gateway";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/payments";
        }
    }

    @PostMapping("/{id}/gateway")
    public String gatewayResult(Authentication authentication,
                                @PathVariable Long id,
                                @RequestParam boolean approve,
                                RedirectAttributes redirect) {
        User user = currentUserService.get(authentication);
        try {
            Payment payment = paymentService.completeCheckout(user, id, approve);
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                redirect.addFlashAttribute("success", "Mokėjimas sėkmingas. Paketas „"
                        + payment.getPackagePurchase().getLessonPackage().getName() + "“ aktyvuotas.");
            } else {
                redirect.addFlashAttribute("error",
                        "Mokėjimas nepavyko, paketas neaktyvuotas. Pabandykite dar kartą arba "
                                + "pasirinkite kitą mokėjimo būdą.");
            }
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payments";
    }
}
