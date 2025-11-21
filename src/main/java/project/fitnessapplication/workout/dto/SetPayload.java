package project.fitnessapplication.workout.dto;

import lombok.Data;
import project.fitnessapplication.workout.model.SetGroupType;

import java.util.UUID;

@Data
public class SetPayload {
    private Double weight;
    private Integer reps;
    private UUID groupId;
    private SetGroupType groupType;
    private Integer groupOrder;
    private Integer setNumber;
}

