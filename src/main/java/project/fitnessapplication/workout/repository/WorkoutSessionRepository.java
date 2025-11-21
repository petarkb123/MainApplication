package project.fitnessapplication.workout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplication.workout.model.WorkoutSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    List<WorkoutSession> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    List<WorkoutSession> findByUserIdOrderByStartedAtDesc(UUID userId);

    List<WorkoutSession> findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(
            UUID userId, LocalDateTime from, LocalDateTime to);

    List<WorkoutSession> findTop50ByUserIdOrderByStartedAtDesc(UUID userId);

    List<WorkoutSession> findTop5ByUserIdOrderByStartedAtDesc(UUID userId);


}
