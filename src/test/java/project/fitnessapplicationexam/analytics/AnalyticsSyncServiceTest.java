package project.fitnessapplicationexam.analytics;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.sync.WorkoutSyncRequest;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.model.WorkoutSet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsSyncServiceTest {

    @Mock
    private AnalyticsClient analyticsClient;

    @InjectMocks
    private AnalyticsSyncService analyticsSyncService;

    @Test
    void syncExercise_success() {
        Exercise exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .name("Bench Press")
                .primaryMuscle(MuscleGroup.CHEST)
                .equipment(Equipment.BARBELL)
                .createdOn(LocalDateTime.now())
                .build();

        doNothing().when(analyticsClient).syncExercises(any());

        analyticsSyncService.syncExercise(exercise);

        verify(analyticsClient).syncExercises(any());
    }

    @Test
    void syncExercise_nullExercise_doesNothing() {
        analyticsSyncService.syncExercise(null);

        verify(analyticsClient, never()).syncExercises(any());
    }

    @Test
    void syncExercise_nullId_doesNothing() {
        Exercise exercise = Exercise.builder()
                .id(null)
                .name("Test")
                .build();

        analyticsSyncService.syncExercise(exercise);

        verify(analyticsClient, never()).syncExercises(any());
    }

    @Test
    void syncExercise_feignException_logsWarning() {
        Exercise exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .name("Bench Press")
                .primaryMuscle(MuscleGroup.CHEST)
                .equipment(Equipment.BARBELL)
                .createdOn(LocalDateTime.now())
                .build();

        doThrow(FeignException.class).when(analyticsClient).syncExercises(any());

        analyticsSyncService.syncExercise(exercise);

        verify(analyticsClient).syncExercises(any());
    }

    @Test
    void syncExercise_genericException_logsWarning() {
        Exercise exercise = Exercise.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .name("Bench Press")
                .primaryMuscle(MuscleGroup.CHEST)
                .equipment(Equipment.BARBELL)
                .createdOn(LocalDateTime.now())
                .build();

        doThrow(RuntimeException.class).when(analyticsClient).syncExercises(any());

        analyticsSyncService.syncExercise(exercise);

        verify(analyticsClient).syncExercises(any());
    }

    @Test
    void deleteExercise_success() {
        UUID exerciseId = UUID.randomUUID();

        doNothing().when(analyticsClient).deleteExercise(exerciseId);

        analyticsSyncService.deleteExercise(exerciseId);

        verify(analyticsClient).deleteExercise(exerciseId);
    }

    @Test
    void deleteExercise_nullId_doesNothing() {
        analyticsSyncService.deleteExercise(null);

        verify(analyticsClient, never()).deleteExercise(any());
    }

    @Test
    void deleteExercise_notFound_logsDebug() {
        UUID exerciseId = UUID.randomUUID();

        doThrow(FeignException.NotFound.class).when(analyticsClient).deleteExercise(exerciseId);

        analyticsSyncService.deleteExercise(exerciseId);

        verify(analyticsClient).deleteExercise(exerciseId);
    }

    @Test
    void deleteExercise_feignException_logsWarning() {
        UUID exerciseId = UUID.randomUUID();

        doThrow(FeignException.class).when(analyticsClient).deleteExercise(exerciseId);

        analyticsSyncService.deleteExercise(exerciseId);

        verify(analyticsClient).deleteExercise(exerciseId);
    }

    @Test
    void syncWorkout_success() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setStartedAt(LocalDateTime.now());
        session.setFinishedAt(LocalDateTime.now());
        session.setStatus(SessionStatus.FINISHED);

        WorkoutSet set = WorkoutSet.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .exerciseId(UUID.randomUUID())
                .reps(10)
                .weight(new BigDecimal("100.0"))
                .build();

        doNothing().when(analyticsClient).syncWorkout(any());

        analyticsSyncService.syncWorkout(session, List.of(set));

        verify(analyticsClient).syncWorkout(any(WorkoutSyncRequest.class));
    }

    @Test
    void syncWorkout_nullSession_doesNothing() {
        analyticsSyncService.syncWorkout(null, List.of());

        verify(analyticsClient, never()).syncWorkout(any());
    }

    @Test
    void syncWorkout_nullSessionId_doesNothing() {
        WorkoutSession session = new WorkoutSession();
        session.setId(null);

        analyticsSyncService.syncWorkout(session, List.of());

        verify(analyticsClient, never()).syncWorkout(any());
    }

    @Test
    void syncWorkout_nullSets_usesEmptyList() {
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());
        session.setStatus(SessionStatus.IN_PROGRESS);

        doNothing().when(analyticsClient).syncWorkout(any());

        analyticsSyncService.syncWorkout(session, null);

        verify(analyticsClient).syncWorkout(argThat(req -> req.sets().isEmpty()));
    }

    @Test
    void syncWorkout_nullStatus_usesDefaultStatus() {
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());
        session.setStatus(null);

        doNothing().when(analyticsClient).syncWorkout(any());

        analyticsSyncService.syncWorkout(session, List.of());

        verify(analyticsClient).syncWorkout(argThat(req -> req.status() == SessionStatus.IN_PROGRESS));
    }

    @Test
    void syncWorkout_feignException_logsWarning() {
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(UUID.randomUUID());
        session.setStatus(SessionStatus.FINISHED);

        doThrow(FeignException.class).when(analyticsClient).syncWorkout(any());

        analyticsSyncService.syncWorkout(session, List.of());

        verify(analyticsClient).syncWorkout(any());
    }

    @Test
    void deleteWorkout_success() {
        UUID sessionId = UUID.randomUUID();

        doNothing().when(analyticsClient).deleteWorkout(sessionId);

        analyticsSyncService.deleteWorkout(sessionId);

        verify(analyticsClient).deleteWorkout(sessionId);
    }

    @Test
    void deleteWorkout_nullId_doesNothing() {
        analyticsSyncService.deleteWorkout(null);

        verify(analyticsClient, never()).deleteWorkout(any());
    }

    @Test
    void deleteWorkout_notFound_logsDebug() {
        UUID sessionId = UUID.randomUUID();

        doThrow(FeignException.NotFound.class).when(analyticsClient).deleteWorkout(sessionId);

        analyticsSyncService.deleteWorkout(sessionId);

        verify(analyticsClient).deleteWorkout(sessionId);
    }

    @Test
    void deleteWorkout_feignException_logsWarning() {
        UUID sessionId = UUID.randomUUID();

        doThrow(FeignException.class).when(analyticsClient).deleteWorkout(sessionId);

        analyticsSyncService.deleteWorkout(sessionId);

        verify(analyticsClient).deleteWorkout(sessionId);
    }
}

