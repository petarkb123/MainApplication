package project.fitnessapplicationexam.analytics.dto;

import java.time.LocalDate;

public record RecomputeWeeklyRequest(
        LocalDate from,
        LocalDate to
) {}


