package project.fitnessapplication.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.fitnessapplication.schedule.model.ScheduledWorkout;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduledWorkoutRepository extends JpaRepository<ScheduledWorkout, UUID> {
    List<ScheduledWorkout> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate from, LocalDate to);
}
