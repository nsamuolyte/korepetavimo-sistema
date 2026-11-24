package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    List<Payout> findByTutorIdOrderByRequestedAtDesc(Long tutorId);

    /** Visų korepetitoriaus išmokų (pateiktų ir išmokėtų) suma (null, jei išmokų nėra). */
    @Query("""
        SELECT SUM(p.amount) FROM Payout p
        WHERE p.tutor.id = :tutorId
        """)
    BigDecimal sumPayouts(@Param("tutorId") Long tutorId);
}
