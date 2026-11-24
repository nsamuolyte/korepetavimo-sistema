package lt.ispk.korepetavimosistema.service;

import java.util.List;
import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final LessonRepository lessonRepository;
    private final GuardianStudentRepository guardianStudentRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            LessonRepository lessonRepository,
            GuardianStudentRepository guardianStudentRepository) {

        this.reviewRepository = reviewRepository;
        this.lessonRepository = lessonRepository;
        this.guardianStudentRepository = guardianStudentRepository;
    }

    public boolean canReview(Lesson lesson, User author) {

        if (lesson.getStatus() != LessonStatus.COMPLETED) {
            return false;
        }

        boolean student = author.getRole() == UserRole.STUDENT
                && lesson.getStudent().getId().equals(author.getId());

        boolean guardian = author.getRole() == UserRole.GUARDIAN
                && guardianStudentRepository
                .findByGuardianIdAndStudentId(
                        author.getId(),
                        lesson.getStudent().getId())
                .filter(link -> link.getStatus() == LinkStatus.APPROVED)
                .isPresent();

        return (student || guardian)
                && !reviewRepository.existsByLessonIdAndAuthorId(
                lesson.getId(), author.getId());
    }

    @Transactional
    public void addReview(Long lessonId, User author,
                          int rating, String comment) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pamoka nerasta"));

        if (!canReview(lesson, author)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Šios pamokos įvertinti negalima");
        }

        if (rating < 1 || rating > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Įvertinimas turi būti nuo 1 iki 10");
        }

        if (comment != null && comment.length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Komentaras per ilgas");
        }

        Review review = new Review();
        review.setLesson(lesson);
        review.setAuthor(author);
        review.setRating(rating);
        review.setComment(comment);

        reviewRepository.save(review);
    }

    public Double getAverageRating(Long tutorId) {
        return reviewRepository.findAverageRating(tutorId);
    }

    public long getReviewCount(Long tutorId) {
        return reviewRepository.countByLessonTutorId(tutorId);
    }

    public List<Review> getReviews(Long tutorId) {
        return reviewRepository
                .findByLessonTutorIdOrderByCreatedAtDesc(tutorId);
    }
}