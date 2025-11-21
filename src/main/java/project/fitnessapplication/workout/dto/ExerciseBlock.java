package project.fitnessapplication.workout.dto;

import lombok.Data;
import project.fitnessapplication.exercise.model.Exercise;
import project.fitnessapplication.workout.model.WorkoutSet;

import java.util.List;

@Data
public class ExerciseBlock {
    private final Exercise exercise;
    private final List<WorkoutSet> sets;
}

