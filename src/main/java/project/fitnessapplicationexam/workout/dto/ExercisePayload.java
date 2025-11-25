package project.fitnessapplicationexam.workout.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ExercisePayload {
    @NotNull
    private UUID exerciseId;

    @NotNull
    @Size(min = 1, message = "At least one set is required")
    private List<SetPayload> sets;
}

