package lt.ispk.korepetavimosistema.service;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.*;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pamokų rezervavimas, perkėlimas, atšaukimas ir peržiūra
 * (US-M03, US-M04, US-T03, US-T04, US-K04, FR-16–FR-22).
 */
@Service
public class LessonService {

    /** Mokinys ir globėjas gali atšaukti / perkelti pamoką ne vėliau kaip prieš tiek valandų. */
    public static final int CHANGE_DEADLINE_HOURS = 24;

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final GuardianStudentRepository guardianStudentRepository;
    private final PackagePurchaseRepository packagePurchaseRepository;
    private final AvailabilityService availabilityService;
    private final NotificationService notificationService;

    public LessonService(LessonRepository lessonRepository,
                         UserRepository userRepository,
                         TutorProfileRepository tutorProfileRepository,
                         GuardianStudentRepository guardianStudentRepository,
                         PackagePurchaseRepository packagePurchaseRepository,
                         AvailabilityService availabilityService,
                         NotificationService notificationService) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.guardianStudentRepository = guardianStudentRepository;
        this.packagePurchaseRepository = packagePurchaseRepository;
        this.availabilityService = availabilityService;
        this.notificationService = notificationService;
    }

    // ======================= Rezervavimas =======================

    /**
     * Rezervuoja pamoką.
     *
     * @param actor       kas rezervuoja: mokinys, globėjas arba pats korepetitorius
     * @param tutorUserId korepetitoriaus naudotojo ID
     * @param studentId   mokinio ID (globėjui ir korepetitoriui privalomas, mokiniui ignoruojamas)
     */
    @Transactional
    public Lesson book(User actor, Long tutorUserId, Long studentId,
                       LocalDateTime start, int minutes) {

        if (!AvailabilityService.DURATIONS.contains(minutes)) {
            throw new BookingException("Netinkama pamokos trukmė.");
        }
        if (start == null) {
            throw new BookingException("Pasirinkite pamokos laiką.");
        }

        // Užrakiname korepetitorių, kad lygiagreti rezervacija palauktų (T-05).
        User tutor = userRepository.findByIdForUpdate(tutorUserId)
                .orElseThrow(() -> new BookingException("Korepetitorius nerastas."));

        if (tutor.getRole() != UserRole.TUTOR || !tutor.isActive()) {
            throw new BookingException("Korepetitorius šiuo metu nepasiekiamas.");
        }

        TutorProfile profile = tutorProfileRepository.findByUserId(tutor.getId())
                .filter(p -> p.isApproved() && !p.isRejected())
                .orElseThrow(() -> new BookingException("Korepetitoriaus profilis dar nepatvirtintas."));

        User student = resolveStudent(actor, tutor, studentId);
        LocalDateTime end = start.plusMinutes(minutes);

        validateTime(tutor, student, start, end, -1L);

        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setBookedBy(actor);
        lesson.setSubject(profile.getSubject());
        lesson.setStartTime(start);
        lesson.setEndTime(end);
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setPrice(calculatePrice(profile.getHourlyRate(), minutes));

        // Jei mokinys turi aktyvų paketą pas šį korepetitorių – nurašome vieną pamoką.
        packagePurchaseRepository
                .findByStudentIdAndLessonPackageTutorIdAndActiveTrueAndRemainingLessonsGreaterThanOrderByPurchasedAtAsc(
                        student.getId(), tutor.getId(), 0)
                .stream()
                .findFirst()
                .ifPresent(purchase -> {
                    purchase.setRemainingLessons(purchase.getRemainingLessons() - 1);
                    packagePurchaseRepository.save(purchase);
                    lesson.setPackagePurchase(purchase);
                });

        lessonRepository.save(lesson);

        notifyParticipants(lesson, actor, NotificationType.LESSON_BOOKED,
                fullName(actor) + " rezervavo pamoką " + LtFormat.dateTime(start)
                        + " (" + lesson.getSubject() + ", mokinys " + fullName(student) + ").");

        return lesson;
    }

    /** Pamokos kaina = valandinis įkainis × trukmė valandomis, suapvalinta iki 0,01 EUR (NFR-ST1). */
    public static BigDecimal calculatePrice(BigDecimal hourlyRate, int minutes) {
        return hourlyRate
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private User resolveStudent(User actor, User tutor, Long studentId) {
        return switch (actor.getRole()) {
            case STUDENT -> actor;
            case GUARDIAN -> {
                if (studentId == null) {
                    throw new BookingException("Pasirinkite, kuriam vaikui rezervuojate pamoką.");
                }
                yield approvedChildren(actor).stream()
                        .filter(s -> s.getId().equals(studentId))
                        .findFirst()
                        .orElseThrow(() -> new BookingException(
                                "Pamokas galite rezervuoti tik patvirtintiems susietiems vaikams."));
            }
            case TUTOR -> {
                if (!actor.getId().equals(tutor.getId())) {
                    throw new BookingException("Korepetitorius gali kurti pamokas tik sau.");
                }
                if (studentId == null) {
                    throw new BookingException("Pasirinkite mokinį.");
                }
                User s = userRepository.findById(studentId)
                        .orElseThrow(() -> new BookingException("Mokinys nerastas."));
                if (s.getRole() != UserRole.STUDENT || !s.isActive()) {
                    throw new BookingException("Pasirinktas naudotojas nėra aktyvus mokinys.");
                }
                yield s;
            }
            default -> throw new BookingException("Šis naudotojas negali rezervuoti pamokų.");
        };
    }

    private void validateTime(User tutor, User student, LocalDateTime start,
                              LocalDateTime end, Long excludeLessonId) {
        LocalDateTime now = LocalDateTime.now();

        if (!start.isAfter(now)) {
            throw new BookingException("Pamokos laikas turi būti ateityje.");
        }
        if (start.isAfter(now.plusDays(AvailabilityService.BOOKING_DAYS_AHEAD + 1))) {
            throw new BookingException("Pamoką galima rezervuoti ne vėliau kaip "
                    + AvailabilityService.BOOKING_DAYS_AHEAD + " dienų į priekį.");
        }
        if (!availabilityService.isWithinAvailability(tutor.getId(), start, end)) {
            throw new BookingException("Pasirinktas laikas nepatenka į korepetitoriaus darbo laiką.");
        }
        if (lessonRepository.tutorHasOverlap(tutor.getId(), start, end,
                excludeLessonId, LessonStatus.CANCELLED)) {
            throw new BookingException("Pasirinktas laikas jau užimtas. Pasirinkite kitą laiką.");
        }
        if (lessonRepository.studentHasOverlap(student.getId(), start, end,
                excludeLessonId, LessonStatus.CANCELLED)) {
            throw new BookingException("Mokinys tuo metu jau turi kitą pamoką.");
        }
    }

    // ======================= Atšaukimas ir perkėlimas =======================

    @Transactional
    public Lesson cancel(User actor, Long lessonId, String reason) {
        Lesson lesson = getForUser(actor, lessonId);

        if (lesson.getStatus() != LessonStatus.SCHEDULED) {
            throw new BookingException("Atšaukti galima tik suplanuotą pamoką.");
        }
        checkChangeDeadline(actor, lesson, "atšaukti");

        lesson.setStatus(LessonStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            String trimmed = reason.trim();
            lesson.setCancellationReason(trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed);
        }

        // Grąžiname pamoką į paketą.
        PackagePurchase purchase = lesson.getPackagePurchase();
        if (purchase != null) {
            purchase.setRemainingLessons(purchase.getRemainingLessons() + 1);
            packagePurchaseRepository.save(purchase);
        }

        lessonRepository.save(lesson);

        notifyParticipants(lesson, actor, NotificationType.LESSON_CANCELLED,
                fullName(actor) + " atšaukė pamoką " + LtFormat.dateTime(lesson.getStartTime())
                        + " (" + lesson.getSubject() + ").");
        return lesson;
    }

    @Transactional
    public Lesson reschedule(User actor, Long lessonId, LocalDateTime newStart) {
        Lesson lesson = getForUser(actor, lessonId);

        if (lesson.getStatus() != LessonStatus.SCHEDULED) {
            throw new BookingException("Perkelti galima tik suplanuotą pamoką.");
        }
        if (newStart == null) {
            throw new BookingException("Pasirinkite naują pamokos laiką.");
        }
        checkChangeDeadline(actor, lesson, "perkelti");

        User tutor = userRepository.findByIdForUpdate(lesson.getTutor().getId())
                .orElseThrow(() -> new BookingException("Korepetitorius nerastas."));

        long minutes = Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes();
        LocalDateTime newEnd = newStart.plusMinutes(minutes);

        validateTime(tutor, lesson.getStudent(), newStart, newEnd, lesson.getId());

        LocalDateTime oldStart = lesson.getStartTime();
        lesson.setStartTime(newStart);
        lesson.setEndTime(newEnd);
        lessonRepository.save(lesson);

        notifyParticipants(lesson, actor, NotificationType.LESSON_RESCHEDULED,
                fullName(actor) + " perkėlė pamoką iš " + LtFormat.dateTime(oldStart)
                        + " į " + LtFormat.dateTime(newStart) + " (" + lesson.getSubject() + ").");
        return lesson;
    }

    /** Korepetitorius pažymi, kad pamoka įvyko (naudojama uždarbiui skaičiuoti). */
    @Transactional
    public Lesson complete(User actor, Long lessonId) {
        Lesson lesson = getForUser(actor, lessonId);

        if (!lesson.getTutor().getId().equals(actor.getId())) {
            throw new BookingException("Pamoką įvykusia gali pažymėti tik jos korepetitorius.");
        }
        if (lesson.getStatus() != LessonStatus.SCHEDULED) {
            throw new BookingException("Įvykusia galima pažymėti tik suplanuotą pamoką.");
        }
        if (lesson.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BookingException("Pamoką įvykusia galima pažymėti tik jai prasidėjus.");
        }

        lesson.setStatus(LessonStatus.COMPLETED);
        return lessonRepository.save(lesson);
    }

    private void checkChangeDeadline(User actor, Lesson lesson, String action) {
        if (actor.getRole() == UserRole.TUTOR || actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (LocalDateTime.now().plusHours(CHANGE_DEADLINE_HOURS).isAfter(lesson.getStartTime())) {
            throw new BookingException("Pamoką galima " + action + " ne vėliau kaip prieš "
                    + CHANGE_DEADLINE_HOURS + " val. iki jos pradžios.");
        }
    }

    // ======================= Peržiūra ir teisės =======================

    /** Pamokos, kurias naudotojas mato savo tvarkaraštyje (FR-16, VIR-I3). */
    public List<Lesson> lessonsFor(User user) {
        return switch (user.getRole()) {
            case TUTOR -> lessonRepository.findByTutorIdOrderByStartTimeAsc(user.getId());
            case STUDENT -> lessonRepository.findByStudentIdOrderByStartTimeAsc(user.getId());
            case GUARDIAN -> {
                List<Long> childIds = approvedChildren(user).stream().map(User::getId).toList();
                yield childIds.isEmpty()
                        ? List.of()
                        : lessonRepository.findByStudentIdInOrderByStartTimeAsc(childIds);
            }
            case ADMIN -> lessonRepository.findAll().stream()
                    .sorted(Comparator.comparing(Lesson::getStartTime))
                    .toList();
        };
    }

    /** Pamoka, jei naudotojas turi teisę ją matyti; kitaip – klaida. */
    public Lesson getForUser(User user, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BookingException("Pamoka nerasta."));
        if (!canAccess(user, lesson)) {
            throw new BookingException("Neturite teisės matyti šios pamokos.");
        }
        return lesson;
    }

    public boolean canAccess(User user, Lesson lesson) {
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }
        Long id = user.getId();
        if (lesson.getTutor().getId().equals(id)
                || lesson.getStudent().getId().equals(id)
                || lesson.getBookedBy().getId().equals(id)) {
            return true;
        }
        return user.getRole() == UserRole.GUARDIAN
                && approvedChildren(user).stream()
                .anyMatch(s -> s.getId().equals(lesson.getStudent().getId()));
    }

    /** Patvirtinti, aktyvūs globėjo vaikai. */
    public List<User> approvedChildren(User guardian) {
        return guardianStudentRepository.findByGuardianId(guardian.getId()).stream()
                .filter(link -> link.getStatus() == LinkStatus.APPROVED)
                .map(GuardianStudent::getStudent)
                .filter(User::isActive)
                .toList();
    }

    /** Mokiniai, su kuriais korepetitorius jau turėjo pamokų (pamokos kūrimo formai). */
    public List<User> tutorStudents(User tutor) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (Lesson l : lessonRepository.findByTutorIdOrderByStartTimeAsc(tutor.getId())) {
            if (l.getStudent().isActive()) {
                unique.putIfAbsent(l.getStudent().getId(), l.getStudent());
            }
        }
        return new ArrayList<>(unique.values());
    }

    // ======================= Pranešimai =======================

    /** Priminimų valandos iki pamokos pradžios. */
    public static final int REMINDER_HOURS_BEFORE = 24;

    /**
     * Išsiunčia priminimus apie pamokas, prasidedančias per artimiausias 24 val.
     * Kiekvienai pamokai priminimas siunčiamas tik kartą (US-M04, US-T04, FR-29).
     *
     * @return išsiųstų priminimų (pamokų) skaičius
     */
    @Transactional
    public int sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Lesson> lessons = lessonRepository.findLessonsNeedingReminder(
                LessonStatus.SCHEDULED, now, now.plusHours(REMINDER_HOURS_BEFORE));

        for (Lesson lesson : lessons) {
            String text = "Priminimas: " + lesson.getSubject() + " pamoka "
                    + LtFormat.dateTime(lesson.getStartTime()) + " (korepetitorius "
                    + fullName(lesson.getTutor()) + ", mokinys " + fullName(lesson.getStudent()) + ").";
            notifyParticipants(lesson, null, NotificationType.LESSON_REMINDER, text);
            lesson.setReminderSent(true);
            lessonRepository.save(lesson);
        }
        return lessons.size();
    }

    private void notifyParticipants(Lesson lesson, User actor, NotificationType type, String text) {
        Set<User> recipients = new LinkedHashSet<>();
        recipients.add(lesson.getTutor());
        recipients.add(lesson.getStudent());
        recipients.add(lesson.getBookedBy());
        for (GuardianStudent link : guardianStudentRepository.findByStudentIdAndStatus(
                lesson.getStudent().getId(), LinkStatus.APPROVED)) {
            recipients.add(link.getGuardian());
        }

        Set<Long> notified = new LinkedHashSet<>();
        for (User recipient : recipients) {
            boolean isActor = actor != null && recipient.getId().equals(actor.getId());
            if (isActor || !notified.add(recipient.getId())) {
                continue;
            }
            notificationService.notify(recipient, type, text, "/lessons");
        }
    }

    public static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
