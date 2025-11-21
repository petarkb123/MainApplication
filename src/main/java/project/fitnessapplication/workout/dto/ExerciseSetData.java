package project.fitnessapplication.workout.dto;

import java.util.List;
import java.util.UUID;

public record ExerciseSetData(UUID exerciseId, List<SetData> sets) {}

