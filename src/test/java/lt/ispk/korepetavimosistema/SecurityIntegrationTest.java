package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Registracijos (T-01) ir prieigos teisių (T-03) testai per HTTP sluoksnį.
 */
class SecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("T-01: registracija pavyksta, pakartotinė su tuo pačiu el. paštu – atmetama")
    void registrationRejectsDuplicateEmail() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("firstName", "Jonas")
                        .param("lastName", "Jonaitis")
                        .param("email", "Jonas@Test.lt")
                        .param("password", "Slaptas123")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        mvc.perform(post("/register").with(csrf())
                        .param("firstName", "Kitas")
                        .param("lastName", "Asmuo")
                        .param("email", "jonas@test.lt")
                        .param("password", "Slaptas123")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Šis el. pašto adresas jau naudojamas."));

        assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("Registracija su netinkamu slaptažodžiu atmetama (Bean Validation)")
    void registrationValidatesPassword() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("firstName", "Jonas")
                        .param("lastName", "Jonaitis")
                        .param("email", "jonas@test.lt")
                        .param("password", "abcdefgh")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error",
                        "Slaptažodyje turi būti bent viena raidė ir bent vienas skaitmuo."));

        assertEquals(0, userRepository.count());
    }

    @Test
    @DisplayName("Neprisijungęs naudotojas nukreipiamas į prisijungimo puslapį")
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/lessons"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Korepetitorių paieška pasiekiama be prisijungimo")
    void tutorSearchIsPublic() throws Exception {
        mvc.perform(get("/tutors"))
                .andExpect(status().isOk())
                .andExpect(view().name("tutors"));
    }

    @Test
    @DisplayName("T-03: mokinys negali pasiekti administratoriaus funkcijų")
    void studentCannotAccessAdmin() throws Exception {
        user("student@test.lt", UserRole.STUDENT);

        mvc.perform(get("/admin/users").with(loggedInAs("student@test.lt", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Korepetitorius negali rezervuoti pamokų, mokinys negali keisti darbo laiko")
    void roleRestrictions() throws Exception {
        user("tutor@test.lt", UserRole.TUTOR);
        user("student@test.lt", UserRole.STUDENT);

        mvc.perform(get("/lessons/book/1").with(loggedInAs("tutor@test.lt", "TUTOR")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/tutor/availability").with(loggedInAs("student@test.lt", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Prisijungęs mokinys mato savo tvarkaraštį")
    void studentSeesSchedule() throws Exception {
        user("student@test.lt", UserRole.STUDENT);

        mvc.perform(get("/lessons").with(loggedInAs("student@test.lt", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("lessons"));
    }

    /** Imituoja prisijungusį naudotoją su nurodyta role. */
    private static RequestPostProcessor loggedInAs(String email, String role) {
        return SecurityMockMvcRequestPostProcessors.user(email).roles(role);
    }
}
