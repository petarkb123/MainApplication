package project.fitnessapplicationexam.workout.dto;

import lombok.Data;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.workout.model.WorkoutSet;
import java.util.List;

@Data
public class ExerciseBlock {
    private final Exercise exercise;
    private final List<WorkoutSet> sets;
}

