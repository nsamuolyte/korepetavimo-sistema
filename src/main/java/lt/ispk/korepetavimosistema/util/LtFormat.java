package lt.ispk.korepetavimosistema.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Vieningas datų, laiko ir sumų formatavimas sąsajoje (NFR-VT1, NFR-VT2).
 * Šablonuose naudojama per T(lt.ispk.korepetavimosistema.util.LtFormat).
 */
public final class LtFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private LtFormat() {
    }

    public static String day(DayOfWeek day) {
        if (day == null) {
            return "";
        }
        return switch (day) {
            case MONDAY -> "Pirmadienis";
            case TUESDAY -> "Antradienis";
            case WEDNESDAY -> "Trečiadienis";
            case THURSDAY -> "Ketvirtadienis";
            case FRIDAY -> "Penktadienis";
            case SATURDAY -> "Šeštadienis";
            case SUNDAY -> "Sekmadienis";
        };
    }

    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    /** Pvz. „2026-11-30, Pirmadienis“. */
    public static String dateWithDay(LocalDate date) {
        return date == null ? "" : date.format(DATE) + ", " + day(date.getDayOfWeek());
    }

    public static String time(LocalTime time) {
        return time == null ? "" : time.format(TIME);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(TIME);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME);
    }

    /** Suma eurais su dviem skaitmenimis po kablelio, pvz. „95,00 €“. */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " €";
    }
}
