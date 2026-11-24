package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.LessonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TutorSubjectsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LessonService lessonService;

    private User multiSubjectTutor() {
        User tutor = tutor("tutor@test.lt", "20.00");

        TutorProfile profile = tutorProfileRepository
                .findByUserId(tutor.getId()).orElseThrow();

        profile.setSubjects(Set.of(
                "Matematika", "Fizika", "Informatika"));

        tutorProfileRepository.save(profile);

        return tutor;
    }

    @Test
    @DisplayName("Korepetitoriaus profilyje išsaugomi keli mokomi dalykai")
    void multipleSubjectsAreSaved() {
        User tutor = multiSubjectTutor();

        TutorProfile profile = tutorProfileRepository
                .findByUserId(tutor.getId()).orElseThrow();

        assertEquals(3, profile.getSubjects().size());

        assertEquals(Set.of("Matematika", "Fizika", "Informatika"),
                profile.getSubjects());
    }

    @Test
    @DisplayName("Korepetitorius randamas pagal kiekvieną mokomą dalyką")
    void tutorCanBeFoundByAnySubject() {
        User tutor = multiSubjectTutor();

        for (String subject : Set.of(
                "Matematika", "Fizika", "Informatika")) {

            var results = tutorProfileRepository.searchApprovedTutors(
                    subject, new BigDecimal("100.00"));

            assertEquals(1, results.size());
            assertEquals(tutor.getId(),
                    results.get(0).getUser().getId());
        }
    }

    @Test
    @DisplayName("Korepetitorius nerandamas pagal nemokomą dalyką")
    void unrelatedSubjectIsNotFound() {
        multiSubjectTutor();

        var results = tutorProfileRepository.searchApprovedTutors(
                "Chemija", new BigDecimal("100.00"));

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Mokinys gali rezervuoti pasirinkto dalyko pamoką")
    void studentBooksSelectedSubject() {
        User tutor = multiSubjectTutor();
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonService.book(
                student, tutor.getId(), null,
                lessonStart(10), 60, "Informatika");

        assertEquals("Informatika", lesson.getSubject());
        assertEquals(LessonStatus.SCHEDULED, lesson.getStatus());

        assertEquals("Informatika", lessonRepository
                .findById(lesson.getId()).orElseThrow().getSubject());
    }

    @Test
    @DisplayName("Negalima rezervuoti korepetitoriaus nemokomo dalyko")
    void unsupportedSubjectIsRejected() {
        User tutor = multiSubjectTutor();
        User student = user("student@test.lt", UserRole.STUDENT);

        assertThrows(BookingException.class,
                () -> lessonService.book(
                        student, tutor.getId(), null,
                        lessonStart(10), 60, "Chemija"));

        assertEquals(0, lessonRepository.count());
    }

    @Test
    @DisplayName("Esant keliems dalykams būtina pasirinkti vieną")
    void subjectSelectionIsRequired() {
        User tutor = multiSubjectTutor();
        User student = user("student@test.lt", UserRole.STUDENT);

        assertThrows(BookingException.class,
                () -> lessonService.book(
                        student, tutor.getId(), null,
                        lessonStart(10), 60));

        assertEquals(0, lessonRepository.count());
    }

    @Test
    @DisplayName("Vieną dalyką mokančio korepetitoriaus rezervacija išlieka veikianti")
    void singleSubjectBookingStillWorks() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonService.book(
                student, tutor.getId(), null,
                lessonStart(10), 60);

        assertEquals("Matematika", lesson.getSubject());
    }

    @Test
    @DisplayName("Korepetitorius gali sukurti pasirinkto dalyko pamoką mokiniui")
    void tutorCreatesLessonWithSelectedSubject() {
        User tutor = multiSubjectTutor();
        User student = user("student@test.lt", UserRole.STUDENT);

        lessonDirect(tutor, student, lessonStart(9),
                LessonStatus.SCHEDULED, "20.00");

        Lesson lesson = lessonService.book(
                tutor, tutor.getId(), student.getId(),
                lessonStart(12), 60, "Fizika");

        assertEquals("Fizika", lesson.getSubject());
        assertEquals(student.getId(), lesson.getStudent().getId());
        assertEquals(tutor.getId(), lesson.getBookedBy().getId());
    }
}