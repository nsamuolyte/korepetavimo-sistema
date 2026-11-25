package lt.ispk.korepetavimosistema;

import lt.ispk.korepetavimosistema.controller.AdminController;
import lt.ispk.korepetavimosistema.model.Notification;
import lt.ispk.korepetavimosistema.model.NotificationType;
import lt.ispk.korepetavimosistema.model.TutorProfile;
import lt.ispk.korepetavimosistema.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TutorApprovalNotificationTest extends IntegrationTestBase {

    @Autowired
    private AdminController adminController;

    @Test
    @DisplayName("US-A03 kr. 6: patvirtinus profilį korepetitorius gauna pranešimą")
    void approvalNotifiesTutor() {
        User tutor = tutor("tutor@test.lt", "20.00");
        TutorProfile profile = tutorProfileRepository.findByUserId(tutor.getId()).orElseThrow();

        adminController.approveTutor(profile.getId());

        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(tutor.getId());
        assertEquals(1, list.size());
        assertEquals(NotificationType.TUTOR_PROFILE, list.get(0).getType());
        assertTrue(list.get(0).getText().contains("patvirtintas"));
    }

    @Test
    @DisplayName("US-A03 kr. 5–6: atmestas profilis dingsta iš paieškos, korepetitorius informuojamas")
    void rejectionNotifiesTutorAndHidesProfile() {
        User tutor = tutor("tutor2@test.lt", "20.00");
        TutorProfile profile = tutorProfileRepository.findByUserId(tutor.getId()).orElseThrow();

        adminController.rejectTutor(profile.getId());

        assertTrue(tutorProfileRepository.findById(profile.getId()).orElseThrow().isRejected());
        assertTrue(tutorProfileRepository.findPublicById(profile.getId()).isEmpty());

        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(tutor.getId());
        assertEquals(1, list.size());
        assertTrue(list.get(0).getText().contains("atmestas"));
    }
}