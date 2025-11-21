package project.fitnessapplicationexam.workout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplicationexam.workout.model.WorkoutSession;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    List<WorkoutSession> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    List<WorkoutSession> findTop50ByUserIdOrderByStartedAtDesc(UUID userId);

    List<WorkoutSession> findTop5ByUserIdOrderByStartedAtDesc(UUID userId);


}
