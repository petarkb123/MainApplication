package project.fitnessapplication.schedule.dto;

import project.fitnessapplication.schedule.model.ScheduledWorkout;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduledWorkoutDto(
        UUID id,
        UUID templateId,
        String templateName,
        LocalDate date,
        String notes
) {
    public static ScheduledWorkoutDto fromEntity(ScheduledWorkout sw, String templateName) {
        return new ScheduledWorkoutDto(sw.getId(), sw.getTemplateId(), templateName, sw.getDate(), sw.getNotes());
    }
}
