package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.LessonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pamokų rezervavimo, perkėlimo ir atšaukimo integraciniai testai (FR-17–FR-22, T-05).
 */
class LessonBookingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LessonService lessonService;

    @Test
    @DisplayName("Mokinys rezervuoja laisvą laiką – pamoka sukuriama, kaina apskaičiuojama, korepetitorius gauna pranešimą")
    void studentBooksFreeSlot() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonService.book(student, tutor.getId(), null, lessonStart(10), 90);

        assertEquals(LessonStatus.SCHEDULED, lesson.getStatus());
        assertEquals(new BigDecimal("30.00"), lesson.getPrice());
        assertEquals(lessonStart(10).plusMinutes(90), lesson.getEndTime());
        assertEquals(1, notificationRepository.countByRecipientIdAndReadFalse(tutor.getId()));
    }

    @Test
    @DisplayName("T-05: užimto laiko rezervuoti negalima")
    void occupiedSlotIsRejected() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User s1 = user("s1@test.lt", UserRole.STUDENT);
        User s2 = user("s2@test.lt", UserRole.STUDENT);

        lessonService.book(s1, tutor.getId(), null, lessonStart(10), 60);

        BookingException e = assertThrows(BookingException.class,
                () -> lessonService.book(s2, tutor.getId(), null, lessonStart(10), 60));
        assertTrue(e.getMessage().contains("užimtas"));

        // Iš dalies persidengianti pamoka (10:30–11:30) irgi atmetama.
        assertThrows(BookingException.class,
                () -> lessonService.book(s2, tutor.getId(), null, lessonStart(10).plusMinutes(30), 60));

        // Gretima pamoka (11:00–12:00) leidžiama.
        assertDoesNotThrow(() -> lessonService.book(s2, tutor.getId(), null, lessonStart(11), 60));
    }

    @Test
    @DisplayName("T-05: dvi lygiagrečios rezervacijos į tą patį laiką – patvirtinama tik viena")
    void concurrentBookingsOnlyOneSucceeds() throws Exception {
        User tutor = tutor("tutor@test.lt", "20.00");
        User s1 = user("s1@test.lt", UserRole.STUDENT);
        User s2 = user("s2@test.lt", UserRole.STUDENT);
        LocalDateTime at = lessonStart(14);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);

        Callable<Boolean> bookAsS1 = () -> tryBook(go, s1, tutor, at);
        Callable<Boolean> bookAsS2 = () -> tryBook(go, s2, tutor, at);

        Future<Boolean> f1 = pool.submit(bookAsS1);
        Future<Boolean> f2 = pool.submit(bookAsS2);
        go.countDown();

        int successes = (f1.get(30, TimeUnit.SECONDS) ? 1 : 0) + (f2.get(30, TimeUnit.SECONDS) ? 1 : 0);
        pool.shutdown();

        assertEquals(1, successes);
        assertEquals(1, lessonRepository.count());
    }

    private boolean tryBook(CountDownLatch go, User student, User tutor, LocalDateTime at) throws InterruptedException {
        go.await();
        try {
            lessonService.book(student, tutor.getId(), null, at, 60);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("Rezervuoti ne korepetitoriaus darbo laiku negalima")
    void outsideAvailabilityIsRejected() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        BookingException e = assertThrows(BookingException.class,
                () -> lessonService.book(student, tutor.getId(), null, lessonStart(7), 60));
        assertTrue(e.getMessage().contains("darbo laiką"));

        // 19:30–20:30 išeina už darbo laiko pabaigos (20:00).
        assertThrows(BookingException.class,
                () -> lessonService.book(student, tutor.getId(), null, lessonStart(19).plusMinutes(30), 60));
    }

    @Test
    @DisplayName("Globėjas gali rezervuoti tik patvirtintam susietam vaikui")
    void guardianNeedsApprovedLink() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User guardian = user("guardian@test.lt", UserRole.GUARDIAN);
        User child = user("child@test.lt", UserRole.STUDENT);

        assertThrows(BookingException.class,
                () -> lessonService.book(guardian, tutor.getId(), child.getId(), lessonStart(10), 60));

        link(guardian, child, LinkStatus.APPROVED);
        Lesson lesson = lessonService.book(guardian, tutor.getId(), child.getId(), lessonStart(10), 60);

        assertEquals(child.getId(), lesson.getStudent().getId());
        assertEquals(guardian.getId(), lesson.getBookedBy().getId());
    }

    @Test
    @DisplayName("Pamoka nurašoma iš aktyvaus paketo, o atšaukus – grąžinama")
    void packageLessonIsReturnedOnCancel() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        LessonPackage pkg = lessonPackageRepository.save(
                new LessonPackage(tutor, "5 pamokų paketas", 5, new BigDecimal("95.00")));
        PackagePurchase purchase = new PackagePurchase();
        purchase.setBuyer(student);
        purchase.setStudent(student);
        purchase.setLessonPackage(pkg);
        purchase.setRemainingLessons(2);
        purchase.setActive(true);
        purchase = packagePurchaseRepository.save(purchase);

        Lesson lesson = lessonService.book(student, tutor.getId(), null, lessonStart(10), 60);
        assertEquals(1, packagePurchaseRepository.findById(purchase.getId()).orElseThrow().getRemainingLessons());

        lessonService.cancel(student, lesson.getId(), "Keičiasi planai");
        assertEquals(2, packagePurchaseRepository.findById(purchase.getId()).orElseThrow().getRemainingLessons());
        assertEquals(LessonStatus.CANCELLED, lessonRepository.findById(lesson.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Mokinys negali atšaukti pamokos likus mažiau nei 24 val., korepetitorius – gali")
    void cancellationDeadline() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        Lesson soon = lessonDirect(tutor, student, LocalDateTime.now().plusHours(5),
                LessonStatus.SCHEDULED, "20.00");

        BookingException e = assertThrows(BookingException.class,
                () -> lessonService.cancel(student, soon.getId(), null));
        assertTrue(e.getMessage().contains("24"));

        assertDoesNotThrow(() -> lessonService.cancel(tutor, soon.getId(), "Korepetitorius serga"));
    }

    @Test
    @DisplayName("Pamokos perkėlimas į laisvą laiką pavyksta, į užimtą – ne")
    void reschedule() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User s1 = user("s1@test.lt", UserRole.STUDENT);
        User s2 = user("s2@test.lt", UserRole.STUDENT);

        Lesson l1 = lessonService.book(s1, tutor.getId(), null, lessonStart(10), 60);
        lessonService.book(s2, tutor.getId(), null, lessonStart(12), 60);

        assertThrows(BookingException.class,
                () -> lessonService.reschedule(s1, l1.getId(), lessonStart(12)));

        Lesson moved = lessonService.reschedule(s1, l1.getId(), lessonStart(15));
        assertEquals(lessonStart(15), moved.getStartTime());
        assertEquals(lessonStart(16), moved.getEndTime());
    }

    @Test
    @DisplayName("Svetimos pamokos peržiūrėti negalima")
    void foreignLessonIsNotAccessible() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User s1 = user("s1@test.lt", UserRole.STUDENT);
        User s2 = user("s2@test.lt", UserRole.STUDENT);

        Lesson lesson = lessonService.book(s1, tutor.getId(), null, lessonStart(10), 60);

        assertThrows(BookingException.class, () -> lessonService.getForUser(s2, lesson.getId()));
        assertThrows(BookingException.class, () -> lessonService.cancel(s2, lesson.getId(), null));
    }
}
