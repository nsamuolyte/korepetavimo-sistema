package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.Lesson;
import lt.ispk.korepetavimosistema.model.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByTutorIdOrderByStartTimeAsc(Long tutorId);

    List<Lesson> findByStudentIdOrderByStartTimeAsc(Long studentId);

    List<Lesson> findByStudentIdInOrderByStartTimeAsc(Collection<Long> studentIds);

    List<Lesson> findByTutorIdAndStatusNotAndStartTimeBetweenOrderByStartTimeAsc(
            Long tutorId,
            LessonStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Lesson> findByStatusAndStartTimeBetween(
            LessonStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Ar korepetitorius turi kitą (neatšauktą) pamoką, kuri persidengia
     * su intervalu [start; end). Persidengimas: esama.pradžia < nauja.pabaiga
     * IR esama.pabaiga > nauja.pradžia (FR-19, NFR-ST2).
     * excludeId – pamokos ID, kurią perkeliame (kitaip -1).
     */
    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lesson l
        WHERE l.tutor.id = :tutorId
          AND l.status <> :cancelled
          AND l.id <> :excludeId
          AND l.startTime < :end
          AND l.endTime > :start
        """)
    boolean tutorHasOverlap(
            @Param("tutorId") Long tutorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId,
            @Param("cancelled") LessonStatus cancelled
    );

    /** Tas pats patikrinimas mokiniui – mokinys negali turėti dviejų pamokų vienu metu. */
    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lesson l
        WHERE l.student.id = :studentId
          AND l.status <> :cancelled
          AND l.id <> :excludeId
          AND l.startTime < :end
          AND l.endTime > :start
        """)
    boolean studentHasOverlap(
            @Param("studentId") Long studentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId,
            @Param("cancelled") LessonStatus cancelled
    );

    /** Korepetitoriaus uždarbis – įvykusių pamokų kainų suma (null, jei pamokų nėra). */
    @Query("""
        SELECT SUM(l.price) FROM Lesson l
        WHERE l.tutor.id = :tutorId
          AND l.status = :completed
        """)
    BigDecimal sumEarnings(
            @Param("tutorId") Long tutorId,
            @Param("completed") LessonStatus completed
    );

    /** Suplanuotos pamokos intervale, kurioms dar neišsiųstas priminimas. */
    @Query("""
        SELECT l FROM Lesson l
        WHERE l.status = :scheduled
          AND l.startTime > :from
          AND l.startTime <= :to
          AND (l.reminderSent IS NULL OR l.reminderSent = false)
        """)
    List<Lesson> findLessonsNeedingReminder(
            @Param("scheduled") LessonStatus scheduled,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Ar du naudotojai susiję per bent vieną pamoką (naudojama žinučių teisėms). */
    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lesson l
        WHERE (l.tutor.id = :a AND (l.student.id = :b OR l.bookedBy.id = :b))
           OR (l.tutor.id = :b AND (l.student.id = :a OR l.bookedBy.id = :a))
        """)
    boolean usersShareLesson(@Param("a") Long a, @Param("b") Long b);
}
