package project.fitnessapplication.restday.dto;

import jakarta.validation.constraints.NotNull;
import project.fitnessapplication.restday.model.RestDayReason;

import java.time.LocalDate;

public record CreateRestDayRequest(
        @NotNull LocalDate date,
        @NotNull RestDayReason reason,
        String notes
) {}
