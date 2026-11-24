package lt.ispk.korepetavimosistema.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Formatavimo pagalbininkas Thymeleaf šablonams: ${@fmt.dateTime(...)}, ${@fmt.money(...)}.
 */
@Component("fmt")
public class FormatHelper {

    public String day(DayOfWeek day) {
        return LtFormat.day(day);
    }

    public String date(LocalDate date) {
        return LtFormat.date(date);
    }

    public String dateWithDay(LocalDate date) {
        return LtFormat.dateWithDay(date);
    }

    public String time(LocalTime time) {
        return LtFormat.time(time);
    }

    public String time(LocalDateTime dateTime) {
        return LtFormat.time(dateTime);
    }

    public String dateTime(LocalDateTime dateTime) {
        return LtFormat.dateTime(dateTime);
    }

    public String money(BigDecimal amount) {
        return LtFormat.money(amount);
    }
}
