package project.fitnessapplicationexam.workout.dto;

import project.fitnessapplicationexam.workout.model.SetGroupType;
import java.util.UUID;

public record SetData(
        Double weight,
        Integer reps,
        UUID groupId,
        SetGroupType groupType,
        Integer groupOrder,
        Integer setNumber
) {}

