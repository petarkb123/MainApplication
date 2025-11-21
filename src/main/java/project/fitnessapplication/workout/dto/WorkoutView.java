package project.fitnessapplication.workout.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkoutView {
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final List<ExerciseBlock> exercises;
}

