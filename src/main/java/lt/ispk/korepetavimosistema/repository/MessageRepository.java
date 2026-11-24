package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Visos žinutės tarp dviejų naudotojų chronologine tvarka. */
    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.id = :a AND m.recipient.id = :b)
           OR (m.sender.id = :b AND m.recipient.id = :a)
        ORDER BY m.sentAt ASC
        """)
    List<Message> findConversation(@Param("a") Long a, @Param("b") Long b);

    /** Visos naudotojo žinutės (išsiųstos ir gautos), naujausios pirmos – pokalbių sąrašui. */
    @Query("""
        SELECT m FROM Message m
        WHERE m.sender.id = :userId OR m.recipient.id = :userId
        ORDER BY m.sentAt DESC
        """)
    List<Message> findAllForUser(@Param("userId") Long userId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    boolean existsBySenderIdAndRecipientIdOrSenderIdAndRecipientId(
            Long sender1, Long recipient1, Long sender2, Long recipient2);

    List<Message> findBySenderIdAndRecipientIdAndReadFalse(Long senderId, Long recipientId);
}
