package project.fitnessapplication.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklySummaryDto(
        LocalDate from,
        LocalDate to,
        List<DayStat> days
) {
    public record DayStat(
            LocalDate date,
            int sessions,
            int sets,
            int reps,
            BigDecimal volume
    ) { }
}
