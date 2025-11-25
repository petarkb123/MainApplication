package project.fitnessapplicationexam.analytics.dto.sync;

import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExerciseSyncRequest(
        UUID id,
        UUID ownerUserId,
        String name,
        MuscleGroup primaryMuscle,
        Equipment equipment,
        LocalDateTime createdOn
) {}
