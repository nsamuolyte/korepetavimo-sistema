package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.PackagePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackagePurchaseRepository extends JpaRepository<PackagePurchase, Long> {

    List<PackagePurchase> findByBuyerIdOrderByPurchasedAtDesc(Long buyerId);

    List<PackagePurchase> findByStudentIdOrderByPurchasedAtDesc(Long studentId);

    /** Aktyvūs mokinio paketai pas konkretų korepetitorių, kuriuose dar liko pamokų. */
    List<PackagePurchase>
    findByStudentIdAndLessonPackageTutorIdAndActiveTrueAndRemainingLessonsGreaterThanOrderByPurchasedAtAsc(
            Long studentId,
            Long tutorId,
            int remainingLessons
    );
}
