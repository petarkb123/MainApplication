package project.fitnessapplicationexam.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateMilestoneRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDate achievedDate,
        @NotNull CreateMilestoneRequest.MilestoneType type
) {}

