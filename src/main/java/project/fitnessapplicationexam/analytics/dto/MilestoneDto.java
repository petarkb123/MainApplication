package project.fitnessapplicationexam.analytics.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MilestoneDto(
        UUID id,
        String title,
        String description,
        LocalDate achievedDate,
        CreateMilestoneRequest.MilestoneType type,
        boolean systemGenerated
) {}

