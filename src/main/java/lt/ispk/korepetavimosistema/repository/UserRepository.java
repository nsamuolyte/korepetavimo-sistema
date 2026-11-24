package lt.ispk.korepetavimosistema.repository;

import jakarta.persistence.LockModeType;
import lt.ispk.korepetavimosistema.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdAsc(
            String firstName,
            String lastName,
            String email
    );

    /**
     * Užrakina naudotojo (korepetitoriaus) eilutę iki transakcijos pabaigos.
     * Naudojama rezervuojant pamoką, kad du naudotojai vienu metu
     * negalėtų užimti to paties laiko (FR-19, NFR-P2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
