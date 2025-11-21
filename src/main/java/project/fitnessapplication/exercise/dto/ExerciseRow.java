package project.fitnessapplication.exercise.dto;

import project.fitnessapplication.exercise.model.Equipment;
import project.fitnessapplication.exercise.model.MuscleGroup;

import java.util.UUID;

public record ExerciseRow(
        UUID id,
        String name,
        MuscleGroup primaryMuscle,
        Equipment equipment,
        boolean builtIn
) {}

