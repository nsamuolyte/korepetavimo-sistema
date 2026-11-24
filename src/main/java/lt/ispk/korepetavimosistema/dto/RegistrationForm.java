package lt.ispk.korepetavimosistema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lt.ispk.korepetavimosistema.model.UserRole;

/**
 * Registracijos formos duomenys su Bean Validation (Jakarta Validation 3.1) taisyklėmis.
 */
public class RegistrationForm {

    private static final String NAME_PATTERN = "^[\\p{L}][\\p{L} '\\-]*$";

    @NotBlank(message = "Įveskite vardą.")
    @Size(max = 100, message = "Vardas negali būti ilgesnis nei 100 simbolių.")
    @Pattern(regexp = NAME_PATTERN, message = "Varde gali būti tik raidės, tarpai ir brūkšneliai.")
    private String firstName;

    @NotBlank(message = "Įveskite pavardę.")
    @Size(max = 100, message = "Pavardė negali būti ilgesnė nei 100 simbolių.")
    @Pattern(regexp = NAME_PATTERN, message = "Pavardėje gali būti tik raidės, tarpai ir brūkšneliai.")
    private String lastName;

    @NotBlank(message = "Įveskite el. pašto adresą.")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Neteisingas el. pašto adreso formatas.")
    @Size(max = 255, message = "El. pašto adresas per ilgas.")
    private String email;

    /** BCrypt apdoroja ne daugiau kaip 72 baitus, todėl ribojame ilgį. */
    @NotBlank(message = "Įveskite slaptažodį.")
    @Size(min = 8, max = 64, message = "Slaptažodis turi būti nuo 8 iki 64 simbolių.")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
            message = "Slaptažodyje turi būti bent viena raidė ir bent vienas skaitmuo.")
    private String password;

    @NotNull(message = "Pasirinkite paskyros tipą.")
    private UserRole role;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
