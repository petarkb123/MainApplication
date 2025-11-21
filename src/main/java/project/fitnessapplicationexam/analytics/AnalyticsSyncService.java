package project.fitnessapplicationexam.analytics;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.sync.ExerciseSyncRequest;
import project.fitnessapplicationexam.analytics.dto.sync.WorkoutSetSyncRequest;
import project.fitnessapplicationexam.analytics.dto.sync.WorkoutSyncRequest;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.model.WorkoutSet;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSyncService {

    private final AnalyticsClient analyticsClient;

    public void syncExercise(Exercise exercise) {
        if (exercise == null || exercise.getId() == null) {
            return;
        }
        ExerciseSyncRequest request = new ExerciseSyncRequest(
                exercise.getId(),
                exercise.getOwnerUserId(),
                exercise.getName(),
                exercise.getPrimaryMuscle(),
                exercise.getEquipment(),
                exercise.getCreatedOn()
        );
        try {
            analyticsClient.syncExercises(Collections.singletonList(request));
        } catch (FeignException ex) {
            log.warn("Failed to sync exercise {} with analytics service: {}", exercise.getId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error while syncing exercise {}: {}", exercise.getId(), ex.getMessage());
        }
    }

    public void deleteExercise(UUID exerciseId) {
        if (exerciseId == null) {
            return;
        }
        try {
            analyticsClient.deleteExercise(exerciseId);
        } catch (FeignException.NotFound ex) {
            log.debug("Exercise {} already absent in analytics service", exerciseId);
        } catch (FeignException ex) {
            log.warn("Failed to delete exercise {} from analytics service: {}", exerciseId, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error while deleting exercise {} from analytics service: {}", exerciseId, ex.getMessage());
        }
    }

    public void syncWorkout(WorkoutSession session, List<WorkoutSet> sets) {
        if (session == null || session.getId() == null) {
            return;
        }
        List<WorkoutSetSyncRequest> payload = (sets == null ? List.<WorkoutSetSyncRequest>of() :
                sets.stream()
                        .map(this::mapSet)
                        .collect(Collectors.toList()));
        WorkoutSyncRequest request = new WorkoutSyncRequest(
                session.getId(),
                session.getUserId(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getStatus() != null ? session.getStatus() : SessionStatus.IN_PROGRESS,
                payload
        );
        try {
            analyticsClient.syncWorkout(request);
        } catch (FeignException ex) {
            log.warn("Failed to sync workout {} with analytics service: {}", session.getId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error while syncing workout {}: {}", session.getId(), ex.getMessage());
        }
    }

    public void deleteWorkout(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        try {
            analyticsClient.deleteWorkout(sessionId);
        } catch (FeignException.NotFound ex) {
            log.debug("Workout {} already absent in analytics service", sessionId);
        } catch (FeignException ex) {
            log.warn("Failed to delete workout {} from analytics service: {}", sessionId, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error while deleting workout {} from analytics service: {}", sessionId, ex.getMessage());
        }
    }

    private WorkoutSetSyncRequest mapSet(WorkoutSet set) {
        return new WorkoutSetSyncRequest(
                set.getId(),
                set.getExerciseId(),
                set.getReps(),
                set.getWeight(),
                set.isWarmup(),
                set.getGroupId(),
                set.getGroupType(),
                set.getGroupOrder(),
                set.getSetNumber(),
                set.getExerciseOrder()
        );
    }
}
