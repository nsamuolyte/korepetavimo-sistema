package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayerIdOrderByCreatedAtDesc(Long payerId);
}
