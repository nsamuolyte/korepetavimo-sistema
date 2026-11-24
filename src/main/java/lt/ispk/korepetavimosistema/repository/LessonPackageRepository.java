package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.LessonPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonPackageRepository extends JpaRepository<LessonPackage, Long> {

    List<LessonPackage> findByTutorIdAndActiveTrueOrderByPriceAsc(Long tutorId);

    List<LessonPackage> findByTutorIdOrderByPriceAsc(Long tutorId);
}
