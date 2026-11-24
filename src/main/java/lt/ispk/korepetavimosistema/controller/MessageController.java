package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Susirašinėjimo langai (VIR-F6, VIR-M5).
 */
@Controller
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    public MessageController(MessageService messageService,
                             CurrentUserService currentUserService) {
        this.messageService = messageService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        User me = currentUserService.get(authentication);
        model.addAttribute("conversations", messageService.conversations(me));
        model.addAttribute("contacts", messageService.contacts(me));
        return "messages";
    }

    /** Naujo pokalbio pradžia iš sąrašo (formos GET su partnerId). */
    @GetMapping("/new")
    public String startNew(@RequestParam Long partnerId) {
        return "redirect:/messages/" + partnerId;
    }

    @GetMapping("/{partnerId}")
    public String conversation(Authentication authentication,
                               @PathVariable Long partnerId,
                               Model model,
                               RedirectAttributes redirect) {
        User me = currentUserService.get(authentication);
        try {
            User partner = messageService.partner(me, partnerId);
            model.addAttribute("partner", partner);
            model.addAttribute("messages", messageService.openConversation(me, partner));
            model.addAttribute("me", me);
            model.addAttribute("maxLength", MessageService.MAX_LENGTH);
            return "conversation";
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/messages";
        }
    }

    @PostMapping("/{partnerId}")
    public String send(Authentication authentication,
                       @PathVariable Long partnerId,
                       @RequestParam(required = false) String content,
                       RedirectAttributes redirect) {
        User me = currentUserService.get(authentication);
        try {
            messageService.send(me, partnerId, content);
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/messages/" + partnerId;
    }
}
