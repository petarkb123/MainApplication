package project.fitnessapplicationexam.workout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import project.fitnessapplicationexam.workout.model.SetGroupType;

import java.util.UUID;

@Data
public class SetPayload {
    @PositiveOrZero(message = "Weight must be ≥ 0")
    private Double weight;

    @NotNull(message = "Reps are required")
    @Min(value = 1, message = "Reps must be at least 1")
    private Integer reps;

    private UUID groupId;
    private SetGroupType groupType;
    private Integer groupOrder;
    private Integer setNumber;
}

