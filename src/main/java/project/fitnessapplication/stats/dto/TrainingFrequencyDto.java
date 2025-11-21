package project.fitnessapplication.stats.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TrainingFrequencyDto(
        int totalWorkouts,
        double avgWorkoutsPerWeek,
        Map<String, Integer> workoutsByDayOfWeek,
        List<WeeklyBreakdown> weeklyBreakdown,
        int longestStreak,
        double currentStreak
) {
    public record WeeklyBreakdown(
            LocalDate weekStart,
            LocalDate weekEnd,
            int workoutCount
    ) {}
}

