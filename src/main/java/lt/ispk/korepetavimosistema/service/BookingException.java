package lt.ispk.korepetavimosistema.service;

/**
 * Verslo taisyklės pažeidimas. Pranešimas rodomas naudotojui (VIR-P4),
 * todėl jis turi būti suprantamas ir lietuviškas.
 */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
