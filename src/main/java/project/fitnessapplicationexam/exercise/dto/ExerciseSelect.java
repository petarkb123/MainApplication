
package project.fitnessapplicationexam.exercise.dto;

import project.fitnessapplicationexam.exercise.model.MuscleGroup;

import java.util.UUID;

public record ExerciseSelect(
        UUID id,
        String name,
        MuscleGroup muscleGroup,
        boolean builtIn
) {}

