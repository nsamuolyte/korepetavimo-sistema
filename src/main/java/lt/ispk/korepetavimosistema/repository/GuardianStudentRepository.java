
package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.GuardianStudent;
import lt.ispk.korepetavimosistema.model.LinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianStudentRepository
        extends JpaRepository<GuardianStudent, Long> {

    List<GuardianStudent> findByGuardianId(Long guardianId);

    List<GuardianStudent> findByStudentId(Long studentId);

    Optional<GuardianStudent> findByGuardianIdAndStudentId(
            Long guardianId,
            Long studentId
    );

    List<GuardianStudent> findByStudentIdAndStatus(
            Long studentId,
            LinkStatus status
    );
}
