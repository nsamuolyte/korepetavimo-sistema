package lt.ispk.korepetavimosistema;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lt.ispk.korepetavimosistema.dto.RegistrationForm;
import lt.ispk.korepetavimosistema.model.UserRole;
import lt.ispk.korepetavimosistema.service.LessonService;
import lt.ispk.korepetavimosistema.util.LtFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Modulių (unit) testai be Spring konteksto: kainos skaičiavimas (NFR-ST1),
 * formatavimas (NFR-VT1, NFR-VT2) ir registracijos formos validacija.
 */
class PricingValidationUnitTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest(name = "{0} €/val. × {1} min. = {2} €")
    @CsvSource({
            "20.00, 60, 20.00",
            "20.00, 90, 30.00",
            "22.00, 120, 44.00",
            "19.99, 90, 29.99",
            "18.50, 60, 18.50"
    })
    @DisplayName("Pamokos kaina skaičiuojama tiksliai iki 0,01 EUR")
    void lessonPrice(String rate, int minutes, String expected) {
        assertEquals(new BigDecimal(expected), LessonService.calculatePrice(new BigDecimal(rate), minutes));
    }

    @Test
    @DisplayName("Sumos rodomos eurais su dviem skaitmenimis po kablelio")
    void moneyFormat() {
        assertEquals("95,00 €", LtFormat.money(new BigDecimal("95")));
        assertEquals("18,50 €", LtFormat.money(new BigDecimal("18.5")));
        assertEquals("0,00 €", LtFormat.money(null));
    }

    @Test
    @DisplayName("Data ir laikas rodomi formatu YYYY-MM-DD HH:mm")
    void dateTimeFormat() {
        assertEquals("2026-11-30 09:05", LtFormat.dateTime(LocalDateTime.of(2026, 11, 30, 9, 5)));
        assertEquals("Pirmadienis", LtFormat.day(DayOfWeek.MONDAY));
    }

    @Test
    @DisplayName("Teisinga registracijos forma praeina validaciją")
    void validRegistrationForm() {
        assertTrue(validator.validate(form("Rūta", "Jankauskienė", "ruta@test.lt", "Slaptas123")).isEmpty());
    }

    @Test
    @DisplayName("Netinkami registracijos duomenys atmetami")
    void invalidRegistrationForm() {
        assertFalse(validator.validate(form("Jonas1", "Jonaitis", "jonas@test.lt", "Slaptas123")).isEmpty(),
                "Varde negali būti skaitmenų");
        assertFalse(validator.validate(form("Jonas", "Jonaitis", "netinkamas", "Slaptas123")).isEmpty(),
                "Netinkamas el. paštas");
        assertFalse(validator.validate(form("Jonas", "Jonaitis", "jonas@test.lt", "abc1")).isEmpty(),
                "Per trumpas slaptažodis");

        Set<ConstraintViolation<RegistrationForm>> v =
                validator.validate(form("Jonas", "Jonaitis", "jonas@test.lt", "abcdefgh"));
        assertEquals(1, v.size());
        assertEquals("Slaptažodyje turi būti bent viena raidė ir bent vienas skaitmuo.",
                v.iterator().next().getMessage());
    }

    private static RegistrationForm form(String first, String last, String email, String password) {
        RegistrationForm f = new RegistrationForm();
        f.setFirstName(first);
        f.setLastName(last);
        f.setEmail(email);
        f.setPassword(password);
        f.setRole(UserRole.STUDENT);
        return f;
    }
}
