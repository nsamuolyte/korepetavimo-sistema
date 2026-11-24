package lt.ispk.korepetavimosistema.service;

import lt.ispk.korepetavimosistema.model.*;
import lt.ispk.korepetavimosistema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Susirašinėjimas tarp mokinių, globėjų ir korepetitorių
 * (US-M08, US-T07, US-K08, FR-27, FR-28).
 */
@Service
public class MessageService {

    public static final int MAX_LENGTH = 2000;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final GuardianStudentRepository guardianStudentRepository;
    private final LessonService lessonService;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          LessonRepository lessonRepository,
                          TutorProfileRepository tutorProfileRepository,
                          GuardianStudentRepository guardianStudentRepository,
                          LessonService lessonService,
                          NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.guardianStudentRepository = guardianStudentRepository;
        this.lessonService = lessonService;
        this.notificationService = notificationService;
    }

    /** Pokalbio santrauka pokalbių sąrašui. */
    public record ConversationSummary(User partner, Message lastMessage, long unread) {
        public User getPartner() {
            return partner;
        }

        public Message getLastMessage() {
            return lastMessage;
        }

        public long getUnread() {
            return unread;
        }
    }

    // ======================= Teisės =======================

    /**
     * Ar naudotojas {@code me} gali rašyti naudotojui {@code other}.
     * Leidžiama, jei: vienas jų administratorius; jau yra pokalbis;
     * mokinys / globėjas rašo patvirtintam korepetitoriui; juos sieja pamoka;
     * juos sieja patvirtinta globa; globėjo vaikas turi pamoką pas tą korepetitorių.
     */
    public boolean canMessage(User me, User other) {
        if (other == null || me.getId().equals(other.getId()) || !other.isActive() || !me.isActive()) {
            return false;
        }
        if (me.getRole() == UserRole.ADMIN || other.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (messageRepository.existsBySenderIdAndRecipientIdOrSenderIdAndRecipientId(
                me.getId(), other.getId(), other.getId(), me.getId())) {
            return true;
        }
        boolean meIsClient = me.getRole() == UserRole.STUDENT || me.getRole() == UserRole.GUARDIAN;
        if (meIsClient && other.getRole() == UserRole.TUTOR && isApprovedTutor(other)) {
            return true;
        }
        if (lessonRepository.usersShareLesson(me.getId(), other.getId())) {
            return true;
        }
        if (linkedByGuardianship(me, other)) {
            return true;
        }
        return guardianChildHasLessonWith(me, other) || guardianChildHasLessonWith(other, me);
    }

    private boolean isApprovedTutor(User tutor) {
        return tutorProfileRepository.findByUserId(tutor.getId())
                .map(p -> p.isApproved() && !p.isRejected())
                .orElse(false);
    }

    private boolean linkedByGuardianship(User a, User b) {
        return guardianStudentRepository.findByGuardianIdAndStudentId(a.getId(), b.getId())
                .filter(l -> l.getStatus() == LinkStatus.APPROVED).isPresent()
                || guardianStudentRepository.findByGuardianIdAndStudentId(b.getId(), a.getId())
                .filter(l -> l.getStatus() == LinkStatus.APPROVED).isPresent();
    }

    private boolean guardianChildHasLessonWith(User guardian, User tutor) {
        if (guardian.getRole() != UserRole.GUARDIAN || tutor.getRole() != UserRole.TUTOR) {
            return false;
        }
        return lessonService.approvedChildren(guardian).stream()
                .anyMatch(child -> lessonRepository.usersShareLesson(child.getId(), tutor.getId()));
    }

    // ======================= Pokalbiai =======================

    /** Visi naudotojo pokalbiai, naujausias viršuje. */
    public List<ConversationSummary> conversations(User me) {
        Map<Long, ConversationSummary> byPartner = new LinkedHashMap<>();
        Map<Long, Long> unread = new LinkedHashMap<>();

        for (Message m : messageRepository.findAllForUser(me.getId())) {
            User partner = m.getSender().getId().equals(me.getId()) ? m.getRecipient() : m.getSender();
            byPartner.putIfAbsent(partner.getId(), new ConversationSummary(partner, m, 0));
            if (m.getRecipient().getId().equals(me.getId()) && !m.isRead()) {
                unread.merge(partner.getId(), 1L, Long::sum);
            }
        }

        List<ConversationSummary> result = new ArrayList<>();
        for (ConversationSummary c : byPartner.values()) {
            result.add(new ConversationSummary(c.partner(), c.lastMessage(),
                    unread.getOrDefault(c.partner().getId(), 0L)));
        }
        return result;
    }

    /** Naudotojai, kuriems galima pradėti naują pokalbį (pagal pamokas ir globą). */
    public List<User> contacts(User me) {
        Map<Long, User> candidates = new LinkedHashMap<>();

        for (Lesson l : lessonService.lessonsFor(me)) {
            candidates.putIfAbsent(l.getTutor().getId(), l.getTutor());
            candidates.putIfAbsent(l.getStudent().getId(), l.getStudent());
            candidates.putIfAbsent(l.getBookedBy().getId(), l.getBookedBy());
            if (me.getRole() == UserRole.TUTOR) {
                for (GuardianStudent g : guardianStudentRepository.findByStudentIdAndStatus(
                        l.getStudent().getId(), LinkStatus.APPROVED)) {
                    candidates.putIfAbsent(g.getGuardian().getId(), g.getGuardian());
                }
            }
        }
        if (me.getRole() == UserRole.GUARDIAN) {
            for (User child : lessonService.approvedChildren(me)) {
                candidates.putIfAbsent(child.getId(), child);
            }
        }
        if (me.getRole() == UserRole.STUDENT) {
            for (GuardianStudent g : guardianStudentRepository.findByStudentIdAndStatus(
                    me.getId(), LinkStatus.APPROVED)) {
                candidates.putIfAbsent(g.getGuardian().getId(), g.getGuardian());
            }
        }

        return candidates.values().stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .filter(u -> u.getRole() != UserRole.ADMIN)
                .filter(u -> canMessage(me, u))
                .sorted(Comparator.comparing(User::getFirstName))
                .toList();
    }

    public User partner(User me, Long partnerId) {
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new BookingException("Naudotojas nerastas."));
        if (!canMessage(me, partner)) {
            throw new BookingException("Su šiuo naudotoju susirašinėti negalite.");
        }
        return partner;
    }

    /** Pokalbio žinutės; gautos žinutės pažymimos kaip perskaitytos. */
    @Transactional
    public List<Message> openConversation(User me, User partner) {
        List<Message> unread = messageRepository
                .findBySenderIdAndRecipientIdAndReadFalse(partner.getId(), me.getId());
        for (Message m : unread) {
            m.setRead(true);
        }
        messageRepository.saveAll(unread);
        return messageRepository.findConversation(me.getId(), partner.getId());
    }

    @Transactional
    public Message send(User me, Long partnerId, String content) {
        User partner = partner(me, partnerId);

        if (content == null || content.isBlank()) {
            throw new BookingException("Žinutė negali būti tuščia.");
        }
        String text = content.trim();
        if (text.length() > MAX_LENGTH) {
            throw new BookingException("Žinutė per ilga (daugiausia " + MAX_LENGTH + " simbolių).");
        }

        Message message = messageRepository.save(new Message(me, partner, text));

        String preview = text.length() > 60 ? text.substring(0, 60) + "…" : text;
        notificationService.notify(partner, NotificationType.NEW_MESSAGE,
                "Nauja žinutė nuo " + LessonService.fullName(me) + ": „" + preview + "“",
                "/messages/" + me.getId());
        return message;
    }

    public long unreadCount(User me) {
        return messageRepository.countByRecipientIdAndReadFalse(me.getId());
    }
}
