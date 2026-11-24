package lt.ispk.korepetavimosistema.service;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.LessonRepository;
import lt.ispk.korepetavimosistema.repository.PayoutRepository;
import lt.ispk.korepetavimosistema.repository.UserRepository;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Korepetitoriaus uždarbis ir išmokos (US-K10, FR-30).
 * Uždarbis = įvykusių pamokų kainų suma; galima išmokėti = uždarbis − jau pateiktos išmokos.
 */
@Service
public class PayoutService {

    private final LessonRepository lessonRepository;
    private final PayoutRepository payoutRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PayoutService(LessonRepository lessonRepository,
                         PayoutRepository payoutRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.lessonRepository = lessonRepository;
        this.payoutRepository = payoutRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public record EarningsSummary(BigDecimal earned, BigDecimal paidOut, BigDecimal available) {
        // Getteriai Thymeleaf šablonams (${summary.earned} ir t. t.).
        public BigDecimal getEarned() {
            return earned;
        }

        public BigDecimal getPaidOut() {
            return paidOut;
        }

        public BigDecimal getAvailable() {
            return available;
        }
    }

    public EarningsSummary summary(User tutor) {
        BigDecimal earned = orZero(lessonRepository.sumEarnings(tutor.getId(), LessonStatus.COMPLETED));
        BigDecimal paidOut = orZero(payoutRepository.sumPayouts(tutor.getId()));
        return new EarningsSummary(earned, paidOut, earned.subtract(paidOut));
    }

    public List<Payout> history(User tutor) {
        return payoutRepository.findByTutorIdOrderByRequestedAtDesc(tutor.getId());
    }

    @Transactional
    public Payout requestPayout(User actor, BigDecimal amount) {
        if (actor.getRole() != UserRole.TUTOR) {
            throw new BookingException("Išmoką inicijuoti gali tik korepetitorius.");
        }
        // Užrakiname, kad du lygiagretūs prašymai neviršytų galimos sumos.
        User tutor = userRepository.findByIdForUpdate(actor.getId())
                .orElseThrow(() -> new BookingException("Naudotojas nerastas."));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BookingException("Išmokos suma turi būti didesnė už 0.");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal available = summary(tutor).available();
        if (amount.compareTo(available) > 0) {
            throw new BookingException("Galima išmokėti ne daugiau kaip " + LtFormat.money(available) + ".");
        }

        Payout payout = new Payout();
        payout.setTutor(tutor);
        payout.setAmount(amount);
        payout.setStatus(PayoutStatus.REQUESTED);
        payoutRepository.save(payout);

        notificationService.notify(tutor, NotificationType.PAYOUT,
                "Išmokos prašymas " + LtFormat.money(amount) + " pateiktas.", "/tutor/earnings");
        return payout;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
