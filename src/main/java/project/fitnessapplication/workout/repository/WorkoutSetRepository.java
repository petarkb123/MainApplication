package project.fitnessapplication.workout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplication.workout.model.WorkoutSet;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {

    List<WorkoutSet> findAllBySessionIdIn(Collection<UUID> sessionIds);

    void deleteBySessionId(UUID sessionId);

    List<WorkoutSet> findAllBySessionIdOrderByExerciseOrderAscIdAsc(UUID sessionId);
    
    List<WorkoutSet> findAllBySessionId(UUID sessionId);
}
