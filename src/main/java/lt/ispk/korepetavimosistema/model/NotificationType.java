package lt.ispk.korepetavimosistema.model;

/**
 * Pranešimo tipas.
 */
public enum NotificationType {
    LESSON_BOOKED("Nauja rezervacija"),
    LESSON_RESCHEDULED("Pamoka perkelta"),
    LESSON_CANCELLED("Pamoka atšaukta"),
    LESSON_REMINDER("Priminimas"),
    NEW_MESSAGE("Nauja žinutė"),
    PAYMENT("Mokėjimas"),
    PAYOUT("Išmoka"),
    GUARDIAN_LINK("Globos susiejimas");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
