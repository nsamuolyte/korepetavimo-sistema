
package lt.ispk.korepetavimosistema.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "guardian_students",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"guardian_id", "student_id"}
        )
)
public class GuardianStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private User guardian;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkStatus status = LinkStatus.PENDING;

    public GuardianStudent() {
    }

    public Long getId() {
        return id;
    }

    public User getGuardian() {
        return guardian;
    }

    public void setGuardian(User guardian) {
        this.guardian = guardian;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public void setStatus(LinkStatus status) {
        this.status = status;
    }
}
