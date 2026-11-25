package lt.ispk.korepetavimosistema.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * Mokėjimų sistemos imitacija testinei aplinkai.
 * Nesaugo ir negauna kortelės duomenų (NFR-A5).
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public String createSession(BigDecimal amount, String description) {
        String id = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        log.info("Imituota mokėjimo sesija {} sukurta: {} EUR, {}", id, amount, description);
        return id;
    }

    @Override
    public PaymentResult confirm(String externalId, boolean approved) {
        log.info("Imituotos mokėjimo sesijos {} rezultatas: {}", externalId, approved ? "PATVIRTINTA" : "ATMESTA");
        return approved
                ? new PaymentResult(true, "Mokėjimas patvirtintas.")
                : new PaymentResult(false, "Mokėjimų sistema atmetė mokėjimą.");
    }
}
