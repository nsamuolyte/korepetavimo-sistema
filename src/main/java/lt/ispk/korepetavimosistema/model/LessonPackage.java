package lt.ispk.korepetavimosistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Korepetitoriaus siūlomas pamokų paketas (FR-23).
 */
@Entity
@Table(name = "lesson_packages")
public class LessonPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private User tutor;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int lessonCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    public LessonPackage() {
    }

    public LessonPackage(User tutor, String name, int lessonCount, BigDecimal price) {
        this.tutor = tutor;
        this.name = name;
        this.lessonCount = lessonCount;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public User getTutor() {
        return tutor;
    }

    public void setTutor(User tutor) {
        this.tutor = tutor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(int lessonCount) {
        this.lessonCount = lessonCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
