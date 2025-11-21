package project.fitnessapplicationexam.analytics.dto.sync;

import project.fitnessapplicationexam.workout.model.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WorkoutSyncRequest(
        UUID id,
        UUID userId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        SessionStatus status,
        List<WorkoutSetSyncRequest> sets
) {}
