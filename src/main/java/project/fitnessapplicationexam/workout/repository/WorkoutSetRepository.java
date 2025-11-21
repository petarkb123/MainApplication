package project.fitnessapplicationexam.workout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplicationexam.workout.model.WorkoutSet;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {

    void deleteBySessionId(UUID sessionId);

    List<WorkoutSet> findAllBySessionIdOrderByExerciseOrderAscIdAsc(UUID sessionId);
    
    List<WorkoutSet> findAllBySessionId(UUID sessionId);
    
    void deleteByExerciseId(UUID exerciseId);
}
