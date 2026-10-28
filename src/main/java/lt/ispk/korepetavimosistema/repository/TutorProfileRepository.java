
package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.TutorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TutorProfileRepository
        extends JpaRepository<TutorProfile, Long> {

    Optional<TutorProfile> findByUserId(Long userId);

    List<TutorProfile> findByApprovedFalseAndRejectedFalse();

    @Query("""
        SELECT t FROM TutorProfile t
        JOIN FETCH t.user u
        WHERE t.approved = true
          AND t.rejected = false
          AND u.active = true
          AND LOWER(t.subject) LIKE LOWER(
              CONCAT('%', :subject, '%')
          )
          AND t.hourlyRate <= :maxPrice
        ORDER BY t.hourlyRate ASC
        """)
    List<TutorProfile> searchApprovedTutors(
            @Param("subject") String subject,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query("""
        SELECT t FROM TutorProfile t
        JOIN FETCH t.user u
        WHERE t.id = :id
          AND t.approved = true
          AND t.rejected = false
          AND u.active = true
        """)
    Optional<TutorProfile> findPublicById(@Param("id") Long id);
}
