package project.fitnessapplicationexam.template.dto;

import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import java.util.UUID;

public record ExerciseOptionDto(
        UUID id,
        String name,
        MuscleGroup muscleGroup,
        UUID ownerUserId
) {}

