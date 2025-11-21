package project.fitnessapplication.workout.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ExercisePayload {
    private UUID exerciseId;
    private List<SetPayload> sets;
}

