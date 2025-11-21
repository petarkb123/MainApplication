package project.fitnessapplication.template.dto;

import project.fitnessapplication.exercise.model.MuscleGroup;

import java.util.UUID;

public record ExerciseOptionDto(
        UUID id,
        String name,
        MuscleGroup muscleGroup,
        UUID ownerUserId
) {}

