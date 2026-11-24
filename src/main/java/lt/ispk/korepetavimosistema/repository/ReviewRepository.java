package lt.ispk.korepetavimosistema.repository;

import lt.ispk.korepetavimosistema.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByLessonIdAndAuthorId(Long lessonId, Long authorId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.lesson.tutor.id = :tutorId")
    Double findAverageRating(@Param("tutorId") Long tutorId);

    long countByLessonTutorId(Long tutorId);

    List<Review> findByLessonTutorIdOrderByCreatedAtDesc(Long tutorId);
}
