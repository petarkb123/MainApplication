package project.fitnessapplicationexam.common.schedulers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksTest {

    @Mock
    private WorkoutSessionRepository sessionRepository;

    @Mock
    private WorkoutSetRepository setRepository;

    @InjectMocks
    private ScheduledTasks scheduledTasks;

    @Test
    void cleanupAbandonedWorkoutSessions_removesOldSessions() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        WorkoutSession abandonedSession = new WorkoutSession();
        abandonedSession.setId(sessionId);
        abandonedSession.setUserId(userId);
        abandonedSession.setStatus(SessionStatus.IN_PROGRESS);
        abandonedSession.setStartedAt(LocalDateTime.now().minusDays(10));

        when(sessionRepository.findAll()).thenReturn(List.of(abandonedSession));
        doNothing().when(setRepository).deleteBySessionId(sessionId);
        doNothing().when(sessionRepository).delete(abandonedSession);

        scheduledTasks.cleanupAbandonedWorkoutSessions();

        verify(setRepository).deleteBySessionId(sessionId);
        verify(sessionRepository).delete(abandonedSession);
    }

    @Test
    void cleanupAbandonedWorkoutSessions_ignoresRecentSessions() {
        WorkoutSession recentSession = new WorkoutSession();
        recentSession.setStatus(SessionStatus.IN_PROGRESS);
        recentSession.setStartedAt(LocalDateTime.now().minusDays(1));

        when(sessionRepository.findAll()).thenReturn(List.of(recentSession));

        scheduledTasks.cleanupAbandonedWorkoutSessions();

        verify(setRepository, never()).deleteBySessionId(any());
        verify(sessionRepository, never()).delete(any());
    }

    @Test
    void cleanupAbandonedWorkoutSessions_ignoresFinishedSessions() {
        WorkoutSession finishedSession = new WorkoutSession();
        finishedSession.setStatus(SessionStatus.FINISHED);
        finishedSession.setStartedAt(LocalDateTime.now().minusDays(10));

        when(sessionRepository.findAll()).thenReturn(List.of(finishedSession));

        scheduledTasks.cleanupAbandonedWorkoutSessions();

        verify(setRepository, never()).deleteBySessionId(any());
        verify(sessionRepository, never()).delete(any());
    }
}

