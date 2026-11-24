package lt.ispk.korepetavimosistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Įsigytas pamokų paketas. Aktyvuojamas tik sėkmingai apmokėjus (FR-25, NFR-R2).
 */
@Entity
@Table(name = "package_purchases")
public class PackagePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kas moka: mokinys arba globėjas. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /** Kuriam mokiniui skirtas paketas. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_package_id", nullable = false)
    private LessonPackage lessonPackage;

    @Column(nullable = false)
    private int remainingLessons;

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private LocalDateTime purchasedAt = LocalDateTime.now();

    public PackagePurchase() {
    }

    public Long getId() {
        return id;
    }

    public User getBuyer() {
        return buyer;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LessonPackage getLessonPackage() {
        return lessonPackage;
    }

    public void setLessonPackage(LessonPackage lessonPackage) {
        this.lessonPackage = lessonPackage;
    }

    public int getRemainingLessons() {
        return remainingLessons;
    }

    public void setRemainingLessons(int remainingLessons) {
        this.remainingLessons = remainingLessons;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(LocalDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }
}
