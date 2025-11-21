package project.fitnessapplication.template.dto;

import project.fitnessapplication.exercise.model.MuscleGroup;
import project.fitnessapplication.workout.model.SetGroupType;

import java.util.UUID;

public record ExerciseOption(
        UUID id,
        String name,
        MuscleGroup muscleGroup,
        Integer targetSets,
        UUID groupId,
        SetGroupType groupType,
        Integer groupOrder,
        Integer position,
        Integer setNumber
) {}

