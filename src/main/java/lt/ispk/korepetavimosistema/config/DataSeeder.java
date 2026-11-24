package lt.ispk.korepetavimosistema.config;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Užpildo duomenų bazę demonstraciniais duomenimis (po 3–5 įrašus kiekvienoje lentelėje).
 * Vykdomas tik vieną kartą: jei naudotojas admin@demo.lt jau yra, nieko nedaroma.
 * Visų demonstracinių paskyrų slaptažodis: Demo12345
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String MARKER_EMAIL = "admin@demo.lt";
    public static final String DEMO_PASSWORD = "Demo12345";

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final GuardianStudentRepository guardianStudentRepository;
    private final AvailabilityRepository availabilityRepository;
    private final LessonRepository lessonRepository;
    private final LessonPackageRepository lessonPackageRepository;
    private final PackagePurchaseRepository packagePurchaseRepository;
    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      TutorProfileRepository tutorProfileRepository,
                      GuardianStudentRepository guardianStudentRepository,
                      AvailabilityRepository availabilityRepository,
                      LessonRepository lessonRepository,
                      LessonPackageRepository lessonPackageRepository,
                      PackagePurchaseRepository packagePurchaseRepository,
                      PaymentRepository paymentRepository,
                      PayoutRepository payoutRepository,
                      MessageRepository messageRepository,
                      NotificationRepository notificationRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.guardianStudentRepository = guardianStudentRepository;
        this.availabilityRepository = availabilityRepository;
        this.lessonRepository = lessonRepository;
        this.lessonPackageRepository = lessonPackageRepository;
        this.packagePurchaseRepository = packagePurchaseRepository;
        this.paymentRepository = paymentRepository;
        this.payoutRepository = payoutRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(MARKER_EMAIL)) {
            log.info("Demonstraciniai duomenys jau yra – praleidžiama.");
            return;
        }

        log.info("Kuriami demonstraciniai duomenys...");

        // ---------- Naudotojai ----------
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        user("Sistemos", "Administratorius", MARKER_EMAIL, UserRole.ADMIN, hash);

        User jonas = user("Jonas", "Petraitis", "jonas@demo.lt", UserRole.TUTOR, hash);
        User ruta = user("Rūta", "Jankauskienė", "ruta@demo.lt", UserRole.TUTOR, hash);
        User tomas = user("Tomas", "Žukauskas", "tomas@demo.lt", UserRole.TUTOR, hash);
        User egle = user("Eglė", "Vaitkutė", "egle@demo.lt", UserRole.TUTOR, hash);

        User lukas = user("Lukas", "Kazlauskas", "lukas@demo.lt", UserRole.STUDENT, hash);
        User ieva = user("Ieva", "Stankevičiūtė", "ieva@demo.lt", UserRole.STUDENT, hash);
        User matas = user("Matas", "Paulauskas", "matas@demo.lt", UserRole.STUDENT, hash);
        User gabija = user("Gabija", "Urbonaitė", "gabija@demo.lt", UserRole.STUDENT, hash);

        User asta = user("Asta", "Kazlauskienė", "asta@demo.lt", UserRole.GUARDIAN, hash);
        User darius = user("Darius", "Stankevičius", "darius@demo.lt", UserRole.GUARDIAN, hash);
        User rasa = user("Rasa", "Paulauskienė", "rasa@demo.lt", UserRole.GUARDIAN, hash);

        // ---------- Korepetitorių profiliai ----------
        profile(jonas, "Matematika", "9–12 klasės", "20.00", 6, true,
                "Ruošiu matematikos VBE, aiškinu nuo pagrindų iki sudėtingų uždavinių.");
        profile(ruta, "Anglų kalba", "5–10 klasės", "18.00", 8, true,
                "Pokalbiai, gramatika ir pasiruošimas PUPP.");
        profile(tomas, "Fizika", "9–12 klasės", "22.00", 4, true,
                "Fizikos uždaviniai, laboratoriniai darbai ir VBE pasiruošimas.");
        profile(egle, "Chemija", "8–12 klasės", "19.00", 2, false,
                "Neorganinė ir organinė chemija moksleiviams.");

        // ---------- Globėjų ir mokinių ryšiai ----------
        link(asta, lukas, LinkStatus.APPROVED);
        link(darius, ieva, LinkStatus.APPROVED);
        link(rasa, matas, LinkStatus.PENDING);

        // ---------- Darbo laikas ----------
        availability(jonas, DayOfWeek.MONDAY, 15, 19);
        availability(jonas, DayOfWeek.THURSDAY, 15, 19);
        availability(ruta, DayOfWeek.WEDNESDAY, 14, 19);
        availability(ruta, DayOfWeek.FRIDAY, 14, 18);
        availability(tomas, DayOfWeek.SATURDAY, 9, 14);
        availability(tomas, DayOfWeek.TUESDAY, 16, 20);

        // ---------- Pamokų paketai ----------
        LessonPackage jonas5 = lessonPackage(jonas, "5 pamokų paketas", 5, "95.00");
        lessonPackage(jonas, "10 pamokų paketas", 10, "180.00");
        LessonPackage ruta5 = lessonPackage(ruta, "5 pamokų paketas", 5, "85.00");
        LessonPackage tomas4 = lessonPackage(tomas, "4 pamokų paketas", 4, "84.00");

        // ---------- Įsigyti paketai ir mokėjimai ----------
        PackagePurchase astaPurchase = purchase(asta, lukas, jonas5, 2, true, 20);
        PackagePurchase dariusPurchase = purchase(darius, ieva, ruta5, 3, true, 12);
        PackagePurchase gabijaPurchase = purchase(gabija, gabija, tomas4, 4, false, 2);

        payment(asta, astaPurchase, PaymentStatus.SUCCEEDED, "MOCK-1001", 20);
        payment(darius, dariusPurchase, PaymentStatus.SUCCEEDED, "MOCK-1002", 12);
        payment(gabija, gabijaPurchase, PaymentStatus.FAILED, "MOCK-1003", 2);

        // ---------- Pamokos ----------
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        LocalDate lastWednesday = today.with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
        LocalDate lastSaturday = today.with(TemporalAdjusters.previous(DayOfWeek.SATURDAY));
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextWednesday = today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        LocalDate nextSaturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        lesson(jonas, lukas, asta, "Matematika", lastMonday.minusWeeks(1), 15, 60,
                "20.00", LessonStatus.COMPLETED, astaPurchase);
        lesson(jonas, lukas, asta, "Matematika", lastMonday, 15, 60,
                "20.00", LessonStatus.COMPLETED, astaPurchase);
        lesson(ruta, ieva, darius, "Anglų kalba", lastWednesday, 16, 60,
                "18.00", LessonStatus.COMPLETED, dariusPurchase);
        lesson(tomas, matas, matas, "Fizika", lastSaturday, 10, 60,
                "22.00", LessonStatus.COMPLETED, null);

        Lesson upcomingJonas = lesson(jonas, lukas, asta, "Matematika", nextMonday, 15, 60,
                "20.00", LessonStatus.SCHEDULED, astaPurchase);
        Lesson upcomingRuta = lesson(ruta, ieva, ieva, "Anglų kalba", nextWednesday, 17, 60,
                "18.00", LessonStatus.SCHEDULED, dariusPurchase);
        lesson(tomas, gabija, gabija, "Fizika", nextSaturday, 11, 90,
                "22.00", LessonStatus.SCHEDULED, null);

        Lesson cancelled = lesson(jonas, matas, matas, "Matematika", nextMonday, 17, 60,
                "20.00", LessonStatus.CANCELLED, null);
        cancelled.setCancellationReason("Mokinys susirgo.");
        lessonRepository.save(cancelled);

        // ---------- Išmokos ----------
        payout(jonas, "20.00", PayoutStatus.PAID, 6);
        payout(ruta, "18.00", PayoutStatus.REQUESTED, 1);
        payout(tomas, "22.00", PayoutStatus.PAID, 1);

        // ---------- Žinutės ----------
        message(lukas, jonas, "Laba diena, ar kitą pamoką galėtume pakartoti trupmenas?", 3, true);
        message(jonas, lukas, "Žinoma! Pasiruošk 3 skyriaus uždavinius.", 3, true);
        message(asta, jonas, "Sveiki, kaip Lukui sekasi matematika?", 2, true);
        message(jonas, asta, "Lukas daro gerą pažangą, ypač sprendžiant lygtis.", 2, false);
        message(ieva, ruta, "Ar galite atsiųsti praeitos pamokos žodžių sąrašą?", 1, false);

        // ---------- Pranešimai ----------
        notification(jonas, NotificationType.LESSON_BOOKED,
                "Asta Kazlauskienė rezervavo pamoką mokiniui Lukui "
                        + fmt(upcomingJonas.getStartTime()) + ".", "/lessons", false);
        notification(jonas, NotificationType.LESSON_CANCELLED,
                "Matas Paulauskas atšaukė pamoką " + fmt(cancelled.getStartTime()) + ".",
                "/lessons", false);
        notification(ruta, NotificationType.NEW_MESSAGE,
                "Nauja žinutė nuo Ievos Stankevičiūtės.", "/messages/" + ieva.getId(), false);
        notification(ruta, NotificationType.LESSON_BOOKED,
                "Ieva Stankevičiūtė rezervavo pamoką " + fmt(upcomingRuta.getStartTime()) + ".",
                "/lessons", true);
        notification(asta, NotificationType.PAYMENT,
                "Mokėjimas 95,00 € už „5 pamokų paketas“ sėkmingas.", "/payments", true);
        notification(gabija, NotificationType.PAYMENT,
                "Mokėjimas 84,00 € nepavyko. Paketas neaktyvuotas.", "/payments", false);

        log.info("Demonstraciniai duomenys sukurti. Slaptažodis visoms paskyroms: {}", DEMO_PASSWORD);
    }

    // ================= Pagalbiniai metodai =================

    private User user(String firstName, String lastName, String email,
                      UserRole role, String passwordHash) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }

    private void profile(User tutor, String subject, String level, String rate,
                         int years, boolean approved, String description) {
        TutorProfile p = new TutorProfile();
        p.setUser(tutor);
        p.setSubject(subject);
        p.setEducationLevel(level);
        p.setHourlyRate(new BigDecimal(rate));
        p.setExperienceYears(years);
        p.setDescription(description);
        p.setApproved(approved);
        p.setRejected(false);
        tutorProfileRepository.save(p);
    }

    private void link(User guardian, User student, LinkStatus status) {
        GuardianStudent gs = new GuardianStudent();
        gs.setGuardian(guardian);
        gs.setStudent(student);
        gs.setStatus(status);
        guardianStudentRepository.save(gs);
    }

    private void availability(User tutor, DayOfWeek day, int fromHour, int toHour) {
        availabilityRepository.save(new Availability(
                tutor, day, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0)));
    }

    private LessonPackage lessonPackage(User tutor, String name, int count, String price) {
        return lessonPackageRepository.save(
                new LessonPackage(tutor, name, count, new BigDecimal(price)));
    }

    private PackagePurchase purchase(User buyer, User student, LessonPackage pkg,
                                     int remaining, boolean active, int daysAgo) {
        PackagePurchase pp = new PackagePurchase();
        pp.setBuyer(buyer);
        pp.setStudent(student);
        pp.setLessonPackage(pkg);
        pp.setRemainingLessons(remaining);
        pp.setActive(active);
        pp.setPurchasedAt(LocalDateTime.now().minusDays(daysAgo));
        return packagePurchaseRepository.save(pp);
    }

    private void payment(User payer, PackagePurchase purchase, PaymentStatus status,
                         String externalId, int daysAgo) {
        Payment p = new Payment();
        p.setPayer(payer);
        p.setPackagePurchase(purchase);
        p.setAmount(purchase.getLessonPackage().getPrice());
        p.setStatus(status);
        p.setExternalId(externalId);
        p.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        paymentRepository.save(p);
    }

    private Lesson lesson(User tutor, User student, User bookedBy, String subject,
                          LocalDate date, int hour, int minutes, String hourlyRate,
                          LessonStatus status, PackagePurchase purchase) {
        Lesson l = new Lesson();
        l.setTutor(tutor);
        l.setStudent(student);
        l.setBookedBy(bookedBy);
        l.setSubject(subject);
        l.setStartTime(date.atTime(hour, 0));
        l.setEndTime(date.atTime(hour, 0).plusMinutes(minutes));
        l.setStatus(status);
        l.setPrice(new BigDecimal(hourlyRate)
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        l.setPackagePurchase(purchase);
        l.setCreatedAt(date.atTime(hour, 0).minusDays(5));
        return lessonRepository.save(l);
    }

    private void payout(User tutor, String amount, PayoutStatus status, int daysAgo) {
        Payout p = new Payout();
        p.setTutor(tutor);
        p.setAmount(new BigDecimal(amount));
        p.setStatus(status);
        p.setRequestedAt(LocalDateTime.now().minusDays(daysAgo));
        payoutRepository.save(p);
    }

    private void message(User from, User to, String text, int daysAgo, boolean read) {
        Message m = new Message(from, to, text);
        m.setSentAt(LocalDateTime.now().minusDays(daysAgo).withSecond(0).withNano(0));
        m.setRead(read);
        messageRepository.save(m);
    }

    private void notification(User to, NotificationType type, String text,
                              String link, boolean read) {
        Notification n = new Notification(to, type, text, link);
        n.setRead(read);
        notificationRepository.save(n);
    }

    private static String fmt(LocalDateTime t) {
        return t.toLocalDate() + " " + t.toLocalTime();
    }
}
