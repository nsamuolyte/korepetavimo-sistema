package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.service.BookingException;
import lt.ispk.korepetavimosistema.service.MessageService;
import lt.ispk.korepetavimosistema.service.PaymentService;
import lt.ispk.korepetavimosistema.service.PayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mokėjimų (T-06), išmokų ir žinučių (T-07) integraciniai testai.
 */
class PaymentMessagePayoutIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private MessageService messageService;

    // ======================= Mokėjimai =======================

    @Test
    @DisplayName("T-06: sėkmingas mokėjimas aktyvuoja paketą")
    void successfulPaymentActivatesPackage() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        LessonPackage pkg = paymentService.addPackage(tutor, "5 pamokų paketas", 5, new BigDecimal("95"));

        Payment payment = paymentService.startCheckout(student, pkg.getId(), null);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertFalse(payment.getPackagePurchase().isActive());
        assertNotNull(payment.getExternalId());

        Payment done = paymentService.completeCheckout(student, payment.getId(), true);

        assertEquals(PaymentStatus.SUCCEEDED, done.getStatus());
        PackagePurchase purchase = packagePurchaseRepository
                .findById(done.getPackagePurchase().getId()).orElseThrow();
        assertTrue(purchase.isActive());
        assertEquals(5, purchase.getRemainingLessons());
        assertEquals(new BigDecimal("95.00"), done.getAmount());
    }

    @Test
    @DisplayName("T-06: nepavykęs mokėjimas paketo neaktyvuoja, pakartotinai apdoroti negalima")
    void failedPaymentDoesNotActivatePackage() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        LessonPackage pkg = paymentService.addPackage(tutor, "4 pamokų paketas", 4, new BigDecimal("84.00"));

        Payment payment = paymentService.startCheckout(student, pkg.getId(), null);
        Payment failed = paymentService.completeCheckout(student, payment.getId(), false);

        assertEquals(PaymentStatus.FAILED, failed.getStatus());
        assertFalse(packagePurchaseRepository
                .findById(failed.getPackagePurchase().getId()).orElseThrow().isActive());

        // Jau apdoroto mokėjimo „patvirtinti“ nebegalima.
        assertThrows(BookingException.class,
                () -> paymentService.completeCheckout(student, payment.getId(), true));
    }

    @Test
    @DisplayName("Svetimo mokėjimo apdoroti negalima")
    void foreignPaymentIsRejected() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        User other = user("other@test.lt", UserRole.STUDENT);
        LessonPackage pkg = paymentService.addPackage(tutor, "Paketas", 3, new BigDecimal("60.00"));

        Payment payment = paymentService.startCheckout(student, pkg.getId(), null);

        assertThrows(BookingException.class,
                () -> paymentService.completeCheckout(other, payment.getId(), true));
    }

    // ======================= Išmokos =======================

    @Test
    @DisplayName("Išmokėti galima tik ne daugiau nei uždirbta")
    void payoutCannotExceedEarnings() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        lessonDirect(tutor, student, LocalDateTime.now().minusDays(2), LessonStatus.COMPLETED, "20.00");
        lessonDirect(tutor, student, LocalDateTime.now().minusDays(1), LessonStatus.COMPLETED, "20.00");
        lessonDirect(tutor, student, LocalDateTime.now().plusDays(2), LessonStatus.SCHEDULED, "20.00");

        assertEquals(new BigDecimal("40.00"), payoutService.summary(tutor).available());

        assertThrows(BookingException.class,
                () -> payoutService.requestPayout(tutor, new BigDecimal("50.00")));

        payoutService.requestPayout(tutor, new BigDecimal("15.00"));

        PayoutService.EarningsSummary after = payoutService.summary(tutor);
        assertEquals(new BigDecimal("40.00"), after.earned());
        assertEquals(new BigDecimal("15.00"), after.paidOut());
        assertEquals(new BigDecimal("25.00"), after.available());
    }

    // ======================= Žinutės =======================

    @Test
    @DisplayName("T-07: žinutė išsaugoma, gavėjas ją mato ir gauna pranešimą")
    void messageIsSavedAndVisibleToRecipient() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);

        messageService.send(student, tutor.getId(), "Laba diena, ar turite laiko penktadienį?");

        assertEquals(1, messageRepository.countByRecipientIdAndReadFalse(tutor.getId()));
        assertEquals(1, notificationRepository.countByRecipientIdAndReadFalse(tutor.getId()));

        var conversation = messageService.openConversation(tutor, student);
        assertEquals(1, conversation.size());
        assertEquals("Laba diena, ar turite laiko penktadienį?", conversation.getFirst().getContent());

        // Atidarius pokalbį žinutė tampa perskaityta.
        assertEquals(0, messageRepository.countByRecipientIdAndReadFalse(tutor.getId()));
    }

    @Test
    @DisplayName("Nesusijusiam naudotojui rašyti negalima, tuščios žinutės siųsti negalima")
    void messagingRules() {
        User tutor = tutor("tutor@test.lt", "20.00");
        User student = user("student@test.lt", UserRole.STUDENT);
        User otherStudent = user("other@test.lt", UserRole.STUDENT);

        assertThrows(BookingException.class,
                () -> messageService.send(student, otherStudent.getId(), "Labas"));
        assertThrows(BookingException.class,
                () -> messageService.send(student, tutor.getId(), "   "));
        assertEquals(0, messageRepository.count());
    }
}
