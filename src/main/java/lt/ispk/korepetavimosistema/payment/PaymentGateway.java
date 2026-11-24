package lt.ispk.korepetavimosistema.payment;

import java.math.BigDecimal;

/**
 * Išorinės mokėjimų sistemos sąsaja (FR-24).
 * Prototipe naudojama {@link MockPaymentGateway} – tikrų finansinių operacijų nėra.
 * Produkcinėje aplinkoje šią sąsają realizuotų tikro mokėjimų tiekėjo klientas
 * (HTTP API, JSON – NFR-VI8), o verslo logika nesikeistų.
 */
public interface PaymentGateway {

    /**
     * Sukuria mokėjimo sesiją išorinėje sistemoje.
     *
     * @return išorinės sistemos transakcijos identifikatorius
     */
    String createSession(BigDecimal amount, String description);

    /**
     * Gauna mokėjimo rezultatą iš išorinės sistemos.
     *
     * @param approved prototipe – ar naudotojas imitacijos lange patvirtino mokėjimą
     */
    PaymentResult confirm(String externalId, boolean approved);

    record PaymentResult(boolean success, String message) {
    }
}
