package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Bendra integracinių testų bazė: H2 duomenų bazė (src/test/resources/application.properties),
 * prieš kiekvieną testą išvaloma, ir pagalbiniai testinių duomenų kūrimo metodai.
 */
@SpringBootTest
public abstract class IntegrationTestBase {

    @Autowired protected UserRepository userRepository;
    @Autowired protected TutorProfileRepository tutorProfileRepository;
    @Autowired protected GuardianStudentRepository guardianStudentRepository;
    @Autowired protected AvailabilityRepository availabilityRepository;
    @Autowired protected LessonRepository lessonRepository;
    @Autowired protected LessonPackageRepository lessonPackageRepository;
    @Autowired protected PackagePurchaseRepository packagePurchaseRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected PayoutRepository payoutRepository;
    @Autowired protected MessageRepository messageRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        messageRepository.deleteAll();
        payoutRepository.deleteAll();
        paymentRepository.deleteAll();
        lessonRepository.deleteAll();
        packagePurchaseRepository.deleteAll();
        lessonPackageRepository.deleteAll();
        availabilityRepository.deleteAll();
        guardianStudentRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    /** Pamokų data testams – po 3 dienų (toliau nei 24 val. riba, bet per 14 dienų). */
    protected static LocalDate lessonDate() {
        return LocalDate.now().plusDays(3);
    }

    protected static LocalDateTime lessonStart(int hour) {
        return lessonDate().atTime(hour, 0);
    }

    protected User user(String email, UserRole role) {
        User u = new User();
        u.setFirstName("Testas");
        u.setLastName(role.name().charAt(0) + role.name().substring(1).toLowerCase());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Testas123"));
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }

    /** Patvirtintas korepetitorius, dirbantis lessonDate() savaitės dieną 08:00–20:00. */
    protected User tutor(String email, String hourlyRate) {
        User tutor = user(email, UserRole.TUTOR);

        TutorProfile profile = new TutorProfile();
        profile.setUser(tutor);
        profile.setSubject("Matematika");
        profile.setEducationLevel("9–12 klasės");
        profile.setHourlyRate(new BigDecimal(hourlyRate));
        profile.setExperienceYears(5);
        profile.setDescription("Testinis profilis");
        profile.setApproved(true);
        tutorProfileRepository.save(profile);

        availabilityRepository.save(new Availability(tutor, lessonDate().getDayOfWeek(),
                LocalTime.of(8, 0), LocalTime.of(20, 0)));
        return tutor;
    }

    protected void link(User guardian, User student, LinkStatus status) {
        GuardianStudent gs = new GuardianStudent();
        gs.setGuardian(guardian);
        gs.setStudent(student);
        gs.setStatus(status);
        guardianStudentRepository.save(gs);
    }

    /** Pamoka įrašoma tiesiai į DB (apeinant verslo taisykles) – testų paruošimui. */
    protected Lesson lessonDirect(User tutor, User student, LocalDateTime start,
                                  LessonStatus status, String price) {
        Lesson l = new Lesson();
        l.setTutor(tutor);
        l.setStudent(student);
        l.setBookedBy(student);
        l.setSubject("Matematika");
        l.setStartTime(start);
        l.setEndTime(start.plusHours(1));
        l.setStatus(status);
        l.setPrice(new BigDecimal(price));
        return lessonRepository.save(l);
    }
}
