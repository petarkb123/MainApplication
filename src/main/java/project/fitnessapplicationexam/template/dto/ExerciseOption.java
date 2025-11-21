package project.fitnessapplicationexam.template.dto;

import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.workout.model.SetGroupType;

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

