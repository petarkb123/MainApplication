package project.fitnessapplication.restday.dto;

import project.fitnessapplication.restday.model.RestDay;
import project.fitnessapplication.restday.model.RestDayReason;

import java.time.LocalDate;
import java.util.UUID;

public record RestDayDto(
        UUID id,
        LocalDate date,
        RestDayReason reason,
        String notes
) {
    public static RestDayDto fromEntity(RestDay restDay) {
        return new RestDayDto(
                restDay.getId(),
                restDay.getDate(),
                restDay.getReason(),
                restDay.getNotes()
        );
    }
}
