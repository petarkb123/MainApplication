package project.fitnessapplication.template.dto;

import lombok.Data;
import project.fitnessapplication.workout.model.SetGroupType;

@Data
public class TemplateItemDto {
    private String exerciseId;
    private Integer sets;
    private Integer orderIndex;
    private String groupId;
    private SetGroupType groupType;
    private Integer groupOrder;
    private Integer setNumber;
}

