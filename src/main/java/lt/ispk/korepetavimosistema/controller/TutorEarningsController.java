package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.PaymentService;
import lt.ispk.korepetavimosistema.service.PayoutService;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Korepetitoriaus uždarbis, išmokos (US-K10, FR-30) ir pamokų paketų valdymas (FR-23).
 */
@Controller
@RequestMapping("/tutor")
public class TutorEarningsController {

    private final PayoutService payoutService;
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public TutorEarningsController(PayoutService payoutService,
                                   PaymentService paymentService,
                                   CurrentUserService currentUserService) {
        this.payoutService = payoutService;
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    // ---------- Uždarbis ir išmokos ----------

    @GetMapping("/earnings")
    public String earnings(Authentication authentication, Model model) {
        User tutor = currentUserService.get(authentication);
        model.addAttribute("summary", payoutService.summary(tutor));
        model.addAttribute("payouts", payoutService.history(tutor));
        return "tutor-earnings";
    }

    @PostMapping("/earnings/payout")
    public String requestPayout(Authentication authentication,
                                @RequestParam(required = false) BigDecimal amount,
                                RedirectAttributes redirect) {
        User tutor = currentUserService.get(authentication);
        try {
            var payout = payoutService.requestPayout(tutor, amount);
            redirect.addFlashAttribute("success",
                    "Išmokos prašymas " + LtFormat.money(payout.getAmount()) + " pateiktas.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tutor/earnings";
    }

    // ---------- Pamokų paketai ----------

    @GetMapping("/packages")
    public String packages(Authentication authentication, Model model) {
        User tutor = currentUserService.get(authentication);
        model.addAttribute("packages", paymentService.allPackages(tutor.getId()));
        return "tutor-packages";
    }

    @PostMapping("/packages")
    public String addPackage(Authentication authentication,
                             @RequestParam String name,
                             @RequestParam(defaultValue = "0") int lessonCount,
                             @RequestParam(required = false) BigDecimal price,
                             RedirectAttributes redirect) {
        User tutor = currentUserService.get(authentication);
        try {
            paymentService.addPackage(tutor, name, lessonCount, price);
            redirect.addFlashAttribute("success", "Paketas pridėtas.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tutor/packages";
    }

    @PostMapping("/packages/{id}/toggle")
    public String togglePackage(Authentication authentication,
                                @PathVariable Long id,
                                @RequestParam boolean active,
                                RedirectAttributes redirect) {
        User tutor = currentUserService.get(authentication);
        try {
            paymentService.setPackageActive(tutor, id, active);
            redirect.addFlashAttribute("success", active ? "Paketas vėl parduodamas." : "Paketas paslėptas.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tutor/packages";
    }
}
