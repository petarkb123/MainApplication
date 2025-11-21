package project.fitnessapplicationexam.exercise.dto;

import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;

import java.util.UUID;

public record ExerciseRow(
        UUID id,
        String name,
        MuscleGroup primaryMuscle,
        Equipment equipment,
        boolean builtIn
) {}

