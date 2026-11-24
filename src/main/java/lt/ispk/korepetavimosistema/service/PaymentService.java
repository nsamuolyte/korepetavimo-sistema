package lt.ispk.korepetavimosistema.service;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.payment.PaymentGateway;
import lt.ispk.korepetavimosistema.repository.LessonPackageRepository;
import lt.ispk.korepetavimosistema.repository.PackagePurchaseRepository;
import lt.ispk.korepetavimosistema.repository.PaymentRepository;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pamokų paketai ir jų apmokėjimas (US-M09, US-T08, FR-23–FR-26).
 * Paketas aktyvuojamas tik gavus mokėjimų sistemos patvirtinimą (NFR-R2),
 * būsenų pakeitimai vykdomi vienoje transakcijoje (NFR-P2).
 */
@Service
public class PaymentService {

    private final LessonPackageRepository lessonPackageRepository;
    private final PackagePurchaseRepository packagePurchaseRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final LessonService lessonService;
    private final NotificationService notificationService;

    public PaymentService(LessonPackageRepository lessonPackageRepository,
                          PackagePurchaseRepository packagePurchaseRepository,
                          PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          LessonService lessonService,
                          NotificationService notificationService) {
        this.lessonPackageRepository = lessonPackageRepository;
        this.packagePurchaseRepository = packagePurchaseRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.lessonService = lessonService;
        this.notificationService = notificationService;
    }

    // ======================= Paketai =======================

    public List<LessonPackage> activePackages(Long tutorId) {
        return lessonPackageRepository.findByTutorIdAndActiveTrueOrderByPriceAsc(tutorId);
    }

    public List<LessonPackage> allPackages(Long tutorId) {
        return lessonPackageRepository.findByTutorIdOrderByPriceAsc(tutorId);
    }

    public LessonPackage getActivePackage(Long packageId) {
        LessonPackage pkg = lessonPackageRepository.findById(packageId)
                .orElseThrow(() -> new BookingException("Paketas nerastas."));
        if (!pkg.isActive() || !pkg.getTutor().isActive()) {
            throw new BookingException("Šis paketas šiuo metu neparduodamas.");
        }
        return pkg;
    }

    @Transactional
    public LessonPackage addPackage(User tutor, String name, int lessonCount, BigDecimal price) {
        if (tutor.getRole() != UserRole.TUTOR) {
            throw new BookingException("Paketus kurti gali tik korepetitorius.");
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new BookingException("Įveskite paketo pavadinimą (iki 100 simbolių).");
        }
        if (lessonCount < 1 || lessonCount > 50) {
            throw new BookingException("Pamokų skaičius turi būti nuo 1 iki 50.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BookingException("Paketo kaina turi būti didesnė už 0.");
        }
        return lessonPackageRepository.save(new LessonPackage(
                tutor, name.trim(), lessonCount, price.setScale(2, RoundingMode.HALF_UP)));
    }

    @Transactional
    public void setPackageActive(User tutor, Long packageId, boolean active) {
        LessonPackage pkg = lessonPackageRepository.findById(packageId)
                .orElseThrow(() -> new BookingException("Paketas nerastas."));
        if (!pkg.getTutor().getId().equals(tutor.getId())) {
            throw new BookingException("Negalite keisti kito korepetitoriaus paketo.");
        }
        pkg.setActive(active);
        lessonPackageRepository.save(pkg);
    }

    // ======================= Apmokėjimas =======================

    /**
     * 1 žingsnis: sukuria neaktyvų paketo pirkimą ir PENDING mokėjimą,
     * užregistruoja sesiją išorinėje mokėjimų sistemoje.
     */
    @Transactional
    public Payment startCheckout(User actor, Long packageId, Long studentId) {
        LessonPackage pkg = getActivePackage(packageId);
        User student = resolveStudent(actor, studentId);

        PackagePurchase purchase = new PackagePurchase();
        purchase.setBuyer(actor);
        purchase.setStudent(student);
        purchase.setLessonPackage(pkg);
        purchase.setRemainingLessons(pkg.getLessonCount());
        purchase.setActive(false);
        packagePurchaseRepository.save(purchase);

        Payment payment = new Payment();
        payment.setPayer(actor);
        payment.setPackagePurchase(purchase);
        payment.setAmount(pkg.getPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExternalId(paymentGateway.createSession(pkg.getPrice(),
                pkg.getName() + " – " + LessonService.fullName(pkg.getTutor())));

        return paymentRepository.save(payment);
    }

    /**
     * 2 žingsnis: mokėjimų sistemos atsakymas. Paketas aktyvuojamas tik sėkmės atveju.
     */
    @Transactional
    public Payment completeCheckout(User actor, Long paymentId, boolean approvedByUser) {
        Payment payment = getForPayer(actor, paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BookingException("Šis mokėjimas jau apdorotas.");
        }

        PaymentGateway.PaymentResult result =
                paymentGateway.confirm(payment.getExternalId(), approvedByUser);

        PackagePurchase purchase = payment.getPackagePurchase();
        LessonPackage pkg = purchase.getLessonPackage();
        String amount = LtFormat.money(payment.getAmount());

        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            purchase.setActive(true);
            packagePurchaseRepository.save(purchase);

            notificationService.notify(actor, NotificationType.PAYMENT,
                    "Mokėjimas " + amount + " už „" + pkg.getName() + "“ sėkmingas. Paketas aktyvuotas.",
                    "/payments");
            notificationService.notify(pkg.getTutor(), NotificationType.PAYMENT,
                    LessonService.fullName(actor) + " įsigijo „" + pkg.getName() + "“ mokiniui "
                            + LessonService.fullName(purchase.getStudent()) + ".",
                    "/tutor/earnings");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            notificationService.notify(actor, NotificationType.PAYMENT,
                    "Mokėjimas " + amount + " nepavyko: " + result.message() + " Paketas neaktyvuotas.",
                    "/payments");
        }

        return paymentRepository.save(payment);
    }

    public Payment getForPayer(User actor, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BookingException("Mokėjimas nerastas."));
        if (!payment.getPayer().getId().equals(actor.getId())) {
            throw new BookingException("Neturite teisės matyti šio mokėjimo.");
        }
        return payment;
    }

    private User resolveStudent(User actor, Long studentId) {
        return switch (actor.getRole()) {
            case STUDENT -> actor;
            case GUARDIAN -> {
                if (studentId == null) {
                    throw new BookingException("Pasirinkite, kuriam vaikui perkate paketą.");
                }
                yield lessonService.approvedChildren(actor).stream()
                        .filter(s -> s.getId().equals(studentId))
                        .findFirst()
                        .orElseThrow(() -> new BookingException(
                                "Paketą galite pirkti tik patvirtintam susietam vaikui."));
            }
            default -> throw new BookingException("Paketus gali pirkti mokiniai ir globėjai.");
        };
    }

    // ======================= Istorija =======================

    /** Naudotojo atlikti mokėjimai (FR-26). */
    public List<Payment> paymentHistory(User user) {
        return paymentRepository.findByPayerIdOrderByCreatedAtDesc(user.getId());
    }

    /** Paketai, kuriuos naudotojas pirko arba kurie nupirkti jam (mokiniui – ir globėjo pirkti). */
    public List<PackagePurchase> purchases(User user) {
        Map<Long, PackagePurchase> unique = new LinkedHashMap<>();
        for (PackagePurchase p : packagePurchaseRepository.findByBuyerIdOrderByPurchasedAtDesc(user.getId())) {
            unique.put(p.getId(), p);
        }
        for (PackagePurchase p : packagePurchaseRepository.findByStudentIdOrderByPurchasedAtDesc(user.getId())) {
            unique.putIfAbsent(p.getId(), p);
        }
        List<PackagePurchase> list = new ArrayList<>(unique.values());
        list.sort(Comparator.comparing(PackagePurchase::getPurchasedAt).reversed());
        return list;
    }
}
