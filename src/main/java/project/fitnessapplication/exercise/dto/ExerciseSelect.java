package project.fitnessapplication.exercise.dto;

import project.fitnessapplication.exercise.model.MuscleGroup;

import java.util.UUID;

public record ExerciseSelect(
        UUID id,
        String name,
        MuscleGroup muscleGroup,
        boolean builtIn
) {}

