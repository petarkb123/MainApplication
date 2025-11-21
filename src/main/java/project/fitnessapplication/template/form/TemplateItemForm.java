package project.fitnessapplication.template.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import project.fitnessapplication.workout.model.SetGroupType;

import java.util.UUID;

@Data
public class TemplateItemForm {
    @NotNull(message = "Exercise is required")
    private UUID exerciseId;

    @Min(value = 1, message = "Sets must be at least 1")
    @Max(value = 20, message = "Sets must be ≤ 20")
    private Integer sets = 3;

    @Min(value = 0, message = "Order must be ≥ 0")
    private Integer orderIndex = 0;
    
    private UUID groupId;
    private SetGroupType groupType;
    private Integer groupOrder;
    private Integer setNumber;
}
