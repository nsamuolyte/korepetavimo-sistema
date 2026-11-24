package lt.ispk.korepetavimosistema.model;

/**
 * Mokėjimo būsena.
 */
public enum PaymentStatus {
    PENDING("Laukiama patvirtinimo"),
    SUCCEEDED("Apmokėta"),
    FAILED("Nepavyko");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
