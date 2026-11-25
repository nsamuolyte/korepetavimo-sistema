package lt.ispk.korepetavimosistema.config;

import lt.ispk.korepetavimosistema.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Locale;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Aktyvių sesijų registras – leidžia nutraukti užblokuoto naudotojo sesijas. */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Praneša registrui apie sesijų sukūrimą ir sunaikinimą. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repository) {
        return email -> {
            var user = repository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Naudotojas nerastas"));

            return User.withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .roles(user.getRole().name())
                    .disabled(!user.isActive())
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SessionRegistry sessionRegistry)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/register", "/login",
                        "/css/**", "/js/**", "/images/**",
                        "/error",
                        "/tutors", "/tutors/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/tutor/**").hasRole("TUTOR")
                .requestMatchers("/guardian/**").hasRole("GUARDIAN")
                .requestMatchers("/student/**").hasRole("STUDENT")
                // Rezervuoti pamokas gali mokiniai ir globėjai (US-M03, US-T03).
                .requestMatchers("/lessons/book/**").hasAnyRole("STUDENT", "GUARDIAN")
                // Pirkti paketus gali mokiniai ir globėjai (US-M09, US-T08).
                .requestMatchers("/payments/checkout/**").hasAnyRole("STUDENT", "GUARDIAN")
                .anyRequest().authenticated()
        );

        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
        );

        // Užblokavus naudotoją, administratorius nutraukia jo sesijas;
        // kita užklausa nukreipiama į prisijungimo puslapį (likutis iš 1 sprinto).
        http.sessionManagement(session -> session
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .expiredUrl("/login?blocked")
        );

        http.logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
        );

        return http.build();
    }
}
