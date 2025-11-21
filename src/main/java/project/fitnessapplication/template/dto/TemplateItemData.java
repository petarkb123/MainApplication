package project.fitnessapplication.template.dto;

import project.fitnessapplication.workout.model.SetGroupType;

import java.util.UUID;

public record TemplateItemData(
        UUID exerciseId,
        Integer targetSets,
        Integer position,
        UUID groupId,
        SetGroupType groupType,
        Integer groupOrder,
        Integer setNumber
) {}

