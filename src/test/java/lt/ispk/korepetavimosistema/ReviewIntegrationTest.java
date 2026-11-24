package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReviewService reviewService;

    @Test
    @DisplayName("Mokinys gali įvertinti įvykusią pamoką")
    void studentCanReviewCompletedLesson() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        reviewService.addReview(lesson.getId(), student,
                10, "Puiki pamoka");

        assertEquals(1, reviewService.getReviewCount(tutor.getId()));
        assertEquals(10.0, reviewService.getAverageRating(tutor.getId()), 0.001);
        assertEquals("Puiki pamoka",
                reviewService.getReviews(tutor.getId()).get(0).getComment());
    }

    @Test
    @DisplayName("Neįvykusios pamokos įvertinti negalima")
    void scheduledLessonCannotBeReviewed() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonDirect(tutor, student,
                lessonStart(10), LessonStatus.SCHEDULED, "20.00");

        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), student, 10, null));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        assertEquals(0, reviewRepository.count());
    }

    @Test
    @DisplayName("Tos pačios pamokos du kartus įvertinti negalima")
    void duplicateReviewIsRejected() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        reviewService.addReview(lesson.getId(), student, 8, null);

        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), student, 10, null));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        assertEquals(1, reviewRepository.count());
    }

    @Test
    @DisplayName("Įvertinimas turi būti nuo 1 iki 10")
    void invalidRatingIsRejected() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        ResponseStatusException low = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), student, 0, null));

        ResponseStatusException high = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), student, 11, null));

        assertEquals(HttpStatus.BAD_REQUEST, low.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, high.getStatusCode());
        assertEquals(0, reviewRepository.count());
    }

    @Test
    @DisplayName("Teisingai apskaičiuojamas įvertinimų vidurkis")
    void averageRatingIsCalculatedCorrectly() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson first = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(2),
                LessonStatus.COMPLETED, "20.00");

        Lesson second = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        reviewService.addReview(first.getId(), student, 8, null);
        reviewService.addReview(second.getId(), student, 10, null);

        assertEquals(9.0, reviewService.getAverageRating(tutor.getId()), 0.001);
        assertEquals(2, reviewService.getReviewCount(tutor.getId()));
    }

    @Test
    @DisplayName("Svetimos pamokos įvertinti negalima")
    void foreignLessonCannotBeReviewed() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        User other = user("other@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), other, 10, null));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        assertEquals(0, reviewRepository.count());
    }

    @Test
    @DisplayName("Globėjas gali įvertinti tik patvirtinto susieto vaiko pamoką")
    void guardianNeedsApprovedLink() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        User guardian = user("guardian@test.lt", UserRole.GUARDIAN);

        Lesson lesson = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        assertThrows(ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), guardian, 9, null));

        link(guardian, student, LinkStatus.APPROVED);

        assertDoesNotThrow(() -> reviewService.addReview(
                lesson.getId(), guardian, 9, null));

        assertEquals(1, reviewService.getReviewCount(tutor.getId()));
    }

    @Test
    @DisplayName("Per ilgas atsiliepimo komentaras atmetamas")
    void longCommentIsRejected() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonDirect(tutor, student,
                LocalDateTime.now().minusDays(1),
                LessonStatus.COMPLETED, "20.00");

        ResponseStatusException e = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.addReview(
                        lesson.getId(), student, 10, "a".repeat(1001)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(0, reviewRepository.count());
    }
}