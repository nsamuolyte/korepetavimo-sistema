package lt.ispk.korepetavimosistema.model;

/**
 * Korepetitoriaus išmokos būsena.
 */
public enum PayoutStatus {
    REQUESTED("Pateiktas prašymas"),
    PAID("Išmokėta");

    private final String label;

    PayoutStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
