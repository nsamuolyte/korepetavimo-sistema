package lt.ispk.korepetavimosistema.controller;

import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.service.CurrentUserService;
import lt.ispk.korepetavimosistema.service.ReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(
            ReviewService reviewService,
            CurrentUserService currentUserService) {

        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/add")
    public String addReview(
            @RequestParam Long lessonId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User author = currentUserService.get(authentication);

        reviewService.addReview(lessonId, author, rating, comment);

        redirectAttributes.addFlashAttribute(
                "success", "Įvertinimas sėkmingai pateiktas!");

        return "redirect:/lessons";
    }
}