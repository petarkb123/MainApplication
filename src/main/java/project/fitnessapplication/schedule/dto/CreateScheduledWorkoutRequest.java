package project.fitnessapplication.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateScheduledWorkoutRequest(
        @NotNull UUID templateId,
        @NotNull LocalDate date,
        String notes
) {}
