package project.fitnessapplicationexam.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplicationexam.analytics.dto.sync.ExerciseSyncRequest;
import project.fitnessapplicationexam.analytics.dto.sync.WorkoutSyncRequest;
import project.fitnessapplicationexam.analytics.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "analytics-service", url = "http://localhost:1010/api/analytics")
public interface AnalyticsClient {

    @GetMapping("/weekly")
    ResponseEntity<WeeklySummaryResponse> getWeeklyStats(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @PostMapping("/weekly/recompute")
    void recomputeWeeklyStats(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody RecomputeWeeklyRequest request);

    @GetMapping("/sessions")
    ResponseEntity<List<SessionSummaryResponse>> getSessionSummaries(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/training-frequency")
    ResponseEntity<TrainingFrequencyResponse> getTrainingFrequency(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/volume-trends")
    ResponseEntity<List<ExerciseVolumeTrendDto>> getExerciseVolumeTrends(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/progressive-overload")
    ResponseEntity<List<ProgressiveOverloadDto>> getProgressiveOverload(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/personal-records")
    ResponseEntity<PersonalRecordsDto> getPersonalRecords(
            @RequestHeader("X-User-Id") UUID userId);

    @PostMapping("/milestones")
    void createMilestone(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody CreateMilestoneRequest request);

    @GetMapping("/milestones")
    ResponseEntity<List<MilestoneDto>> getMilestones(
            @RequestHeader("X-User-Id") UUID userId);

    @PutMapping("/milestones/{id}")
    ResponseEntity<MilestoneDto> updateMilestone(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateMilestoneRequest request);

    @DeleteMapping("/milestones/{id}")
    void deleteMilestone(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id);

    @PostMapping("/internal/exercises")
    void syncExercises(@RequestBody List<ExerciseSyncRequest> requests);

    @DeleteMapping("/internal/exercises/{exerciseId}")
    void deleteExercise(@PathVariable UUID exerciseId);

    @PostMapping("/internal/workouts")
    void syncWorkout(@RequestBody WorkoutSyncRequest request);

    @DeleteMapping("/internal/workouts/{workoutId}")
    void deleteWorkout(@PathVariable UUID workoutId);
}

