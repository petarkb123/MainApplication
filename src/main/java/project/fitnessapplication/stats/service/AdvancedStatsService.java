package project.fitnessapplication.stats.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplication.exercise.model.Exercise;
import project.fitnessapplication.exercise.repository.ExerciseRepository;
import project.fitnessapplication.stats.dto.*;
import project.fitnessapplication.workout.model.WorkoutSession;
import project.fitnessapplication.workout.model.WorkoutSet;
import project.fitnessapplication.workout.repository.WorkoutSessionRepository;
import project.fitnessapplication.workout.repository.WorkoutSetRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdvancedStatsService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutSetRepository setRepo;
    private final ExerciseRepository exerciseRepo;

    public AdvancedStatsService(
            WorkoutSessionRepository sessionRepo,
            WorkoutSetRepository setRepo,
            ExerciseRepository exerciseRepo) {
        this.sessionRepo = sessionRepo;
        this.setRepo = setRepo;
        this.exerciseRepo = exerciseRepo;
    }

    /**
     * Get comprehensive training frequency analysis
     */
    public TrainingFrequencyDto getTrainingFrequency(UUID userId, LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay().minusNanos(1);

        List<WorkoutSession> sessions = sessionRepo
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(userId, fromTs, toTs);

        if (sessions.isEmpty()) {
            return new TrainingFrequencyDto(0, 0.0, Map.of(), List.of(), 0, 0.0);
        }

        
        int totalWorkouts = sessions.size();

        
        long daysBetween = ChronoUnit.DAYS.between(from, to) + 1;
        double weeksCount = daysBetween / 7.0;
        double avgPerWeek = totalWorkouts / weeksCount;

        
        Map<String, Integer> byDayOfWeek = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            byDayOfWeek.put(day.name(), 0);
        }
        for (WorkoutSession s : sessions) {
            String dayName = s.getStartedAt().getDayOfWeek().name();
            byDayOfWeek.put(dayName, byDayOfWeek.get(dayName) + 1);
        }

        
        List<TrainingFrequencyDto.WeeklyBreakdown> weeklyBreakdown = new ArrayList<>();
        LocalDate weekStart = from;
        while (!weekStart.isAfter(to)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(to)) weekEnd = to;

            LocalDateTime weekStartTs = weekStart.atStartOfDay();
            LocalDateTime weekEndTs = weekEnd.plusDays(1).atStartOfDay().minusNanos(1);

            long count = sessions.stream()
                    .filter(s -> !s.getStartedAt().isBefore(weekStartTs) && !s.getStartedAt().isAfter(weekEndTs))
                    .count();

            weeklyBreakdown.add(new TrainingFrequencyDto.WeeklyBreakdown(
                    weekStart, weekEnd, (int) count
            ));

            weekStart = weekStart.plusDays(7);
        }

        
        int longestStreak = calculateLongestStreak(sessions, from, to);

        
        double currentStreak = calculateCurrentStreak(sessions, to);

        return new TrainingFrequencyDto(
                totalWorkouts,
                Math.round(avgPerWeek * 10.0) / 10.0,
                byDayOfWeek,
                weeklyBreakdown,
                longestStreak,
                currentStreak
        );
    }

    /**
     * Get exercise-specific volume trends
     */
    public List<ExerciseVolumeTrendDto> getExerciseVolumeTrends(UUID userId, LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay().minusNanos(1);

        List<WorkoutSession> sessions = sessionRepo
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(userId, fromTs, toTs);

        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<UUID, WorkoutSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(WorkoutSession::getId, s -> s));

        List<UUID> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<WorkoutSet> allSets = setRepo.findAllBySessionIdIn(sessionIds);

        
        Map<UUID, List<WorkoutSet>> setsByExercise = allSets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getExerciseId));

        
        Set<UUID> exerciseIds = setsByExercise.keySet();
        Map<UUID, Exercise> exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        List<ExerciseVolumeTrendDto> trends = new ArrayList<>();

        for (Map.Entry<UUID, List<WorkoutSet>> entry : setsByExercise.entrySet()) {
            UUID exerciseId = entry.getKey();
            List<WorkoutSet> sets = entry.getValue();
            Exercise exercise = exerciseMap.get(exerciseId);

            if (exercise == null) continue;

            
            Map<LocalDate, BigDecimal> weeklyVolume = new TreeMap<>();
            Map<LocalDate, Integer> weeklySets = new TreeMap<>();

            for (WorkoutSet set : sets) {
                WorkoutSession session = sessionMap.get(set.getSessionId());
                if (session == null) continue;

                LocalDate weekStart = session.getStartedAt().toLocalDate()
                        .with(DayOfWeek.MONDAY);

                BigDecimal setVolume = BigDecimal.ZERO;
                if (set.getWeight() != null && set.getReps() != null) {
                    setVolume = set.getWeight().multiply(BigDecimal.valueOf(set.getReps()));
                }

                weeklyVolume.merge(weekStart, setVolume, BigDecimal::add);
                weeklySets.merge(weekStart, 1, Integer::sum);
            }

            
            BigDecimal totalVolume = sets.stream()
                    .filter(s -> s.getWeight() != null && s.getReps() != null)
                    .map(s -> s.getWeight().multiply(BigDecimal.valueOf(s.getReps())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalSets = sets.size();
            BigDecimal avgVolumePerSession = totalVolume.divide(
                    BigDecimal.valueOf(weeklySets.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            
            List<LocalDate> weeks = new ArrayList<>(weeklyVolume.keySet());
            String trend = "stable";
            if (weeks.size() >= 2) {
                int midPoint = weeks.size() / 2;
                BigDecimal firstHalfAvg = weeks.subList(0, midPoint).stream()
                        .map(weeklyVolume::get)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(midPoint), 2, RoundingMode.HALF_UP);

                BigDecimal secondHalfAvg = weeks.subList(midPoint, weeks.size()).stream()
                        .map(weeklyVolume::get)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(weeks.size() - midPoint), 2, RoundingMode.HALF_UP);

                if (secondHalfAvg.compareTo(firstHalfAvg.multiply(BigDecimal.valueOf(1.1))) > 0) {
                    trend = "increasing";
                } else if (secondHalfAvg.compareTo(firstHalfAvg.multiply(BigDecimal.valueOf(0.9))) < 0) {
                    trend = "decreasing";
                }
            }

            
            List<ExerciseVolumeTrendDto.WeeklyData> weeklyData = weeklyVolume.entrySet().stream()
                    .map(e -> new ExerciseVolumeTrendDto.WeeklyData(
                            e.getKey(),
                            e.getValue(),
                            weeklySets.get(e.getKey())
                    ))
                    .toList();

            trends.add(new ExerciseVolumeTrendDto(
                    exerciseId,
                    exercise.getName(),
                    exercise.getPrimaryMuscle().name(),
                    totalVolume,
                    totalSets,
                    avgVolumePerSession,
                    trend,
                    weeklyData
            ));
        }

        
        trends.sort((a, b) -> b.totalVolume().compareTo(a.totalVolume()));

        return trends;
    }

    /**
     * Get progressive overload tracking
     */
    public List<ProgressiveOverloadDto> getProgressiveOverload(UUID userId, LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay().minusNanos(1);

        List<WorkoutSession> sessions = sessionRepo
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(userId, fromTs, toTs);

        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<UUID, WorkoutSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(WorkoutSession::getId, s -> s));

        List<UUID> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<WorkoutSet> allSets = setRepo.findAllBySessionIdIn(sessionIds);

        
        Map<UUID, List<WorkoutSet>> setsByExercise = allSets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getExerciseId));

        
        Set<UUID> exerciseIds = setsByExercise.keySet();
        Map<UUID, Exercise> exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        List<ProgressiveOverloadDto> overloads = new ArrayList<>();

        for (Map.Entry<UUID, List<WorkoutSet>> entry : setsByExercise.entrySet()) {
            UUID exerciseId = entry.getKey();
            List<WorkoutSet> sets = entry.getValue();
            Exercise exercise = exerciseMap.get(exerciseId);

            if (exercise == null) continue;

            
            sets.sort((a, b) -> {
                WorkoutSession sa = sessionMap.get(a.getSessionId());
                WorkoutSession sb = sessionMap.get(b.getSessionId());
                if (sa == null || sb == null) return 0;
                return sa.getStartedAt().compareTo(sb.getStartedAt());
            });

            
            List<ProgressiveOverloadDto.ProgressPoint> progressPoints = new ArrayList<>();
            BigDecimal currentMax = BigDecimal.ZERO;

            for (WorkoutSet set : sets) {
                if (set.getWeight() == null) continue;

                WorkoutSession session = sessionMap.get(set.getSessionId());
                if (session == null) continue;

                if (set.getWeight().compareTo(currentMax) > 0) {
                    currentMax = set.getWeight();
                    progressPoints.add(new ProgressiveOverloadDto.ProgressPoint(
                            session.getStartedAt().toLocalDate(),
                            set.getWeight(),
                            set.getReps()
                    ));
                }
            }

            if (progressPoints.isEmpty()) continue;

            
            BigDecimal firstWeight = progressPoints.get(0).weight();
            BigDecimal lastWeight = progressPoints.get(progressPoints.size() - 1).weight();
            double progressPercent = 0.0;
            if (firstWeight.compareTo(BigDecimal.ZERO) > 0) {
                progressPercent = lastWeight.subtract(firstWeight)
                        .divide(firstWeight, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            
            String status = "maintaining";
            if (progressPoints.size() >= 2) {
                LocalDate lastProgressDate = progressPoints.get(progressPoints.size() - 1).date();
                long daysSinceProgress = ChronoUnit.DAYS.between(lastProgressDate, to);

                if (daysSinceProgress <= 14) {
                    status = "progressing";
                } else if (daysSinceProgress > 30) {
                    status = "plateau";
                }
            }

            overloads.add(new ProgressiveOverloadDto(
                    exerciseId,
                    exercise.getName(),
                    exercise.getPrimaryMuscle().name(),
                    progressPoints.get(0).weight(),
                    lastWeight,
                    Math.round(progressPercent * 10.0) / 10.0,
                    status,
                    progressPoints
            ));
        }

        
        overloads.sort((a, b) -> Double.compare(b.progressPercent(), a.progressPercent()));

        return overloads;
    }

    /**
     * Get personal records and milestones
     */
    public PersonalRecordsDto getPersonalRecords(UUID userId) {
        
        List<WorkoutSession> allSessions = sessionRepo.findByUserIdOrderByStartedAtDesc(userId);

        if (allSessions.isEmpty()) {
            return new PersonalRecordsDto(List.of(), List.of());
        }

        List<UUID> sessionIds = allSessions.stream().map(WorkoutSession::getId).toList();
        List<WorkoutSet> allSets = setRepo.findAllBySessionIdIn(sessionIds);

        Map<UUID, WorkoutSession> sessionMap = allSessions.stream()
                .collect(Collectors.toMap(WorkoutSession::getId, s -> s));

        
        Map<UUID, List<WorkoutSet>> setsByExercise = allSets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getExerciseId));

        
        Set<UUID> exerciseIds = setsByExercise.keySet();
        Map<UUID, Exercise> exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        List<PersonalRecordsDto.ExercisePR> exercisePRs = new ArrayList<>();

        for (Map.Entry<UUID, List<WorkoutSet>> entry : setsByExercise.entrySet()) {
            UUID exerciseId = entry.getKey();
            List<WorkoutSet> sets = entry.getValue();
            Exercise exercise = exerciseMap.get(exerciseId);

            if (exercise == null) continue;

            
            WorkoutSet maxWeightSet = sets.stream()
                    .filter(s -> s.getWeight() != null)
                    .max(Comparator.comparing(WorkoutSet::getWeight))
                    .orElse(null);

            
            WorkoutSet maxRepsSet = sets.stream()
                    .filter(s -> s.getReps() != null)
                    .max(Comparator.comparing(WorkoutSet::getReps))
                    .orElse(null);

            
            WorkoutSet maxVolumeSet = sets.stream()
                    .filter(s -> s.getWeight() != null && s.getReps() != null)
                    .max(Comparator.comparing(s -> s.getWeight().multiply(BigDecimal.valueOf(s.getReps()))))
                    .orElse(null);

            if (maxWeightSet != null) {
                WorkoutSession session = sessionMap.get(maxWeightSet.getSessionId());
                exercisePRs.add(new PersonalRecordsDto.ExercisePR(
                        exerciseId,
                        exercise.getName(),
                        "Max Weight",
                        maxWeightSet.getWeight(),
                        maxWeightSet.getReps(),
                        session != null ? session.getStartedAt().toLocalDate() : LocalDate.now()
                ));
            }

            if (maxRepsSet != null && maxRepsSet != maxWeightSet) {
                WorkoutSession session = sessionMap.get(maxRepsSet.getSessionId());
                exercisePRs.add(new PersonalRecordsDto.ExercisePR(
                        exerciseId,
                        exercise.getName(),
                        "Max Reps",
                        maxRepsSet.getWeight(),
                        maxRepsSet.getReps(),
                        session != null ? session.getStartedAt().toLocalDate() : LocalDate.now()
                ));
            }

            if (maxVolumeSet != null && maxVolumeSet != maxWeightSet && maxVolumeSet != maxRepsSet) {
                WorkoutSession session = sessionMap.get(maxVolumeSet.getSessionId());
                exercisePRs.add(new PersonalRecordsDto.ExercisePR(
                        exerciseId,
                        exercise.getName(),
                        "Max Volume (Single Set)",
                        maxVolumeSet.getWeight(),
                        maxVolumeSet.getReps(),
                        session != null ? session.getStartedAt().toLocalDate() : LocalDate.now()
                ));
            }
        }

        
        exercisePRs.sort((a, b) -> b.achievedDate().compareTo(a.achievedDate()));

        
        List<PersonalRecordsDto.Milestone> milestones = new ArrayList<>();

        
        int totalWorkouts = allSessions.size();
        if (totalWorkouts >= 100) milestones.add(new PersonalRecordsDto.Milestone("Century Club", "Completed 100+ workouts", "🏆"));
        else if (totalWorkouts >= 50) milestones.add(new PersonalRecordsDto.Milestone("Half Century", "Completed 50+ workouts", "🎯"));
        else if (totalWorkouts >= 25) milestones.add(new PersonalRecordsDto.Milestone("Quarter Century", "Completed 25+ workouts", "⭐"));
        else if (totalWorkouts >= 10) milestones.add(new PersonalRecordsDto.Milestone("Getting Started", "Completed 10+ workouts", "🌟"));

        
        BigDecimal totalVolume = allSets.stream()
                .filter(s -> s.getWeight() != null && s.getReps() != null)
                .map(s -> s.getWeight().multiply(BigDecimal.valueOf(s.getReps())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalVolume.compareTo(BigDecimal.valueOf(1000000)) >= 0) {
            milestones.add(new PersonalRecordsDto.Milestone("Million Pound Club", "Lifted 1,000,000+ lbs total", "💪"));
        } else if (totalVolume.compareTo(BigDecimal.valueOf(500000)) >= 0) {
            milestones.add(new PersonalRecordsDto.Milestone("Half Million", "Lifted 500,000+ lbs total", "💪"));
        } else if (totalVolume.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            milestones.add(new PersonalRecordsDto.Milestone("100K Club", "Lifted 100,000+ lbs total", "💪"));
        }

        
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long recentWorkouts = allSessions.stream()
                .filter(s -> s.getStartedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .count();

        if (recentWorkouts >= 20) milestones.add(new PersonalRecordsDto.Milestone("Consistency King", "20+ workouts in 30 days", "👑"));
        else if (recentWorkouts >= 12) milestones.add(new PersonalRecordsDto.Milestone("Dedicated", "12+ workouts in 30 days", "🔥"));

        return new PersonalRecordsDto(exercisePRs, milestones);
    }

    
    private int calculateLongestStreak(List<WorkoutSession> sessions, LocalDate from, LocalDate to) {
        if (sessions.isEmpty()) return 0;

        Set<LocalDate> workoutDates = sessions.stream()
                .map(s -> s.getStartedAt().toLocalDate())
                .collect(Collectors.toSet());

        int longestStreak = 0;
        int currentStreak = 0;

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (workoutDates.contains(date)) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        return longestStreak;
    }

    private double calculateCurrentStreak(List<WorkoutSession> sessions, LocalDate to) {
        if (sessions.isEmpty()) return 0.0;

        Set<LocalDate> workoutDates = sessions.stream()
                .map(s -> s.getStartedAt().toLocalDate())
                .collect(Collectors.toSet());

        double streak = 0.0;
        LocalDate date = to;

        while (workoutDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }

        return streak;
    }
}

