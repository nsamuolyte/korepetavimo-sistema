package lt.ispk.korepetavimosistema.config;

import lt.ispk.korepetavimosistema.service.LessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Kas 10 minučių siunčia priminimus apie artėjančias pamokas (US-M04, US-T04).
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final LessonService lessonService;

    public ReminderScheduler(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 600_000)
    public void sendLessonReminders() {
        int sent = lessonService.sendReminders();
        if (sent > 0) {
            log.info("Išsiųsti priminimai apie {} pamok(-as).", sent);
        }
    }
}
