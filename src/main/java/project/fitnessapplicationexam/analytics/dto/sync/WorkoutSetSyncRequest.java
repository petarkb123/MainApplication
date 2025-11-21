package project.fitnessapplicationexam.analytics.dto.sync;

import project.fitnessapplicationexam.workout.model.SetGroupType;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutSetSyncRequest(
        UUID id,
        UUID exerciseId,
        Integer reps,
        BigDecimal weight,
        Boolean warmup,
        UUID groupId,
        SetGroupType groupType,
        Integer groupOrder,
        Integer setNumber,
        Integer exerciseOrder
) {}
