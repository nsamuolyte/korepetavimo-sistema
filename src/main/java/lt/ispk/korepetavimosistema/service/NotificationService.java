package lt.ispk.korepetavimosistema.service;

import lt.ispk.korepetavimosistema.model.Notification;
import lt.ispk.korepetavimosistema.model.NotificationType;
import lt.ispk.korepetavimosistema.model.User;
import lt.ispk.korepetavimosistema.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pranešimų kūrimas ir peržiūra (US-K09, FR-29).
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User recipient, NotificationType type, String text, String link) {
        if (recipient == null || !recipient.isActive()) {
            return;
        }
        notificationRepository.save(new Notification(recipient, type, text, link));
    }

    public List<Notification> forUser(User user) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }
}
