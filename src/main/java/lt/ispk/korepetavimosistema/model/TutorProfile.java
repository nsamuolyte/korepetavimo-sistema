package lt.ispk.korepetavimosistema.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tutor_profiles")
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String subject;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "tutor_subjects",
            joinColumns = @JoinColumn(name = "tutor_profile_id")
    )
    @Column(name = "subject", nullable = false, length = 100)
    private Set<String> subjects = new LinkedHashSet<>();

    @Column(nullable = false)
    private String educationLevel;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(length = 2000)
    private String description;

    private int experienceYears;

    @Column(nullable = false)
    private boolean approved = false;

    @Column(nullable = false)
    private boolean rejected = false;

    public TutorProfile() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
        this.subjects.clear();

        if (subject != null && !subject.isBlank()) {
            this.subjects.add(subject.trim());
        }
    }

    public Set<String> getSubjects() {
        Set<String> result = new LinkedHashSet<>(subjects);

        if (result.isEmpty() && subject != null && !subject.isBlank()) {
            result.add(subject);
        }

        return result;
    }

    public void setSubjects(Set<String> subjects) {
        Set<String> normalized = new LinkedHashSet<>();

        if (subjects != null) {
            for (String value : subjects) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim());
                }
            }
        }

        this.subjects.clear();
        this.subjects.addAll(normalized);

        this.subject = normalized.stream()
                .findFirst()
                .orElse("");
    }

    public String getSubjectsDisplay() {
        return String.join(", ", getSubjects());
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }
}