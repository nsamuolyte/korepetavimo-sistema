package lt.ispk.korepetavimosistema.model;

/**
 * Pamokos būsena (NFR-VT3).
 */
public enum LessonStatus {
    SCHEDULED("Suplanuota"),
    IN_PROGRESS("Vyksta"),
    COMPLETED("Baigta"),
    CANCELLED("Atšaukta");

    private final String label;

    LessonStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
