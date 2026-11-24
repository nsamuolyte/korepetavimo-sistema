package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByTutorIdOrderByDayOfWeekAscStartTimeAsc(Long tutorId);

    List<Availability> findByTutorIdAndDayOfWeek(Long tutorId, DayOfWeek dayOfWeek);
}
