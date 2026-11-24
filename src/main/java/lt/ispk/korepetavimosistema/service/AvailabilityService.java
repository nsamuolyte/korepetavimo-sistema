package lt.ispk.korepetavimosistema.service;

import lt.ispk.korepetavimosistema.model.Availability;
import lt.ispk.korepetavimosistema.model.Lesson;
import lt.ispk.korepetavimosistema.model.LessonStatus;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.model.UserRole;
import lt.ispk.korepetavimosistema.repository.AvailabilityRepository;
import lt.ispk.korepetavimosistema.repository.LessonRepository;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Korepetitoriaus darbo laiko valdymas ir laisvų pamokų laikų skaičiavimas
 * (US-K03, US-M03, FR-15, FR-17).
 */
@Service
public class AvailabilityService {

    /** Kiek dienų į priekį galima rezervuoti pamokas. */
    public static final int BOOKING_DAYS_AHEAD = 14;

    /** Leidžiamos pamokų trukmės minutėmis. */
    public static final List<Integer> DURATIONS = List.of(60, 90, 120);

    /** Laisvų laikų žingsnis minutėmis. */
    public static final int SLOT_STEP_MINUTES = 60;

    private final AvailabilityRepository availabilityRepository;
    private final LessonRepository lessonRepository;

    public AvailabilityService(AvailabilityRepository availabilityRepository,
                               LessonRepository lessonRepository) {
        this.availabilityRepository = availabilityRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<Availability> forTutor(Long tutorId) {
        return availabilityRepository.findByTutorIdOrderByDayOfWeekAscStartTimeAsc(tutorId);
    }

    @Transactional
    public Availability add(User tutor, DayOfWeek day, LocalTime start, LocalTime end) {
        if (tutor.getRole() != UserRole.TUTOR) {
            throw new BookingException("Darbo laiką gali nustatyti tik korepetitorius.");
        }
        if (day == null || start == null || end == null) {
            throw new BookingException("Užpildykite savaitės dieną, pradžios ir pabaigos laiką.");
        }
        if (!start.isBefore(end)) {
            throw new BookingException("Pabaigos laikas turi būti vėlesnis už pradžios laiką.");
        }
        if (Duration.between(start, end).toMinutes() < 60) {
            throw new BookingException("Darbo laiko intervalas turi būti bent 1 valandos.");
        }

        for (Availability existing : availabilityRepository.findByTutorIdAndDayOfWeek(tutor.getId(), day)) {
            if (existing.getStartTime().isBefore(end) && existing.getEndTime().isAfter(start)) {
                throw new BookingException("Šis intervalas persidengia su jau nustatytu darbo laiku ("
                        + LtFormat.day(day) + " "
                        + LtFormat.time(existing.getStartTime()) + "–"
                        + LtFormat.time(existing.getEndTime()) + ").");
            }
        }

        return availabilityRepository.save(new Availability(tutor, day, start, end));
    }

    @Transactional
    public void delete(User tutor, Long availabilityId) {
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new BookingException("Darbo laikas nerastas."));
        if (!availability.getTutor().getId().equals(tutor.getId())) {
            throw new BookingException("Negalite ištrinti kito korepetitoriaus darbo laiko.");
        }
        availabilityRepository.delete(availability);
    }

    /** Ar intervalas [start; end) visas patenka į vieną korepetitoriaus darbo laiko intervalą. */
    public boolean isWithinAvailability(Long tutorId, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }
        for (Availability a : availabilityRepository.findByTutorIdAndDayOfWeek(tutorId, start.getDayOfWeek())) {
            boolean startsInside = !start.toLocalTime().isBefore(a.getStartTime());
            boolean endsInside = !end.toLocalTime().isAfter(a.getEndTime());
            if (startsInside && endsInside) {
                return true;
            }
        }
        return false;
    }

    /**
     * Laisvi pamokų pradžios laikai artimiausioms {@link #BOOKING_DAYS_AHEAD} dienoms.
     * Laikas laisvas, jei patenka į darbo laiką, yra ateityje ir nepersidengia
     * su kita neatšaukta korepetitoriaus pamoka.
     *
     * @param excludeLessonId pamoka, kurią perkeliame (jos laikas laikomas laisvu), arba null
     */
    public Map<LocalDate, List<LocalDateTime>> freeSlots(Long tutorId, int minutes, Long excludeLessonId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate from = now.toLocalDate();
        LocalDate to = from.plusDays(BOOKING_DAYS_AHEAD);

        List<Lesson> busy = lessonRepository
                .findByTutorIdAndStatusNotAndStartTimeBetweenOrderByStartTimeAsc(
                        tutorId, LessonStatus.CANCELLED,
                        from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        List<Availability> availabilities = forTutor(tutorId);
        Map<LocalDate, List<LocalDateTime>> result = new LinkedHashMap<>();

        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<LocalDateTime> daySlots = new ArrayList<>();

            for (Availability a : availabilities) {
                if (a.getDayOfWeek() != day.getDayOfWeek()) {
                    continue;
                }
                LocalDateTime windowEnd = day.atTime(a.getEndTime());

                for (LocalDateTime start = day.atTime(a.getStartTime());
                     !start.plusMinutes(minutes).isAfter(windowEnd);
                     start = start.plusMinutes(SLOT_STEP_MINUTES)) {

                    if (!start.isAfter(now)) {
                        continue;
                    }
                    LocalDateTime slotStart = start;
                    LocalDateTime slotEnd = start.plusMinutes(minutes);

                    boolean taken = busy.stream().anyMatch(l ->
                            (excludeLessonId == null || !l.getId().equals(excludeLessonId))
                                    && l.getStartTime().isBefore(slotEnd)
                                    && l.getEndTime().isAfter(slotStart));

                    if (!taken) {
                        daySlots.add(slotStart);
                    }
                }
            }

            if (!daySlots.isEmpty()) {
                daySlots.sort(null);
                result.put(day, daySlots);
            }
        }
        return result;
    }

    /** Ar korepetitorius turi bent vieną laisvą valandos laiką nurodytą dieną (paieškos filtrui). */
    public boolean hasFreeSlotOn(Long tutorId, LocalDate date) {
        List<LocalDateTime> slots = freeSlots(tutorId, 60, null).get(date);
        return slots != null && !slots.isEmpty();
    }
}
