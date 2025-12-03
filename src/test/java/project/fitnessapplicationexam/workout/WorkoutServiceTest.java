package project.fitnessapplicationexam.workout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplicationexam.analytics.AnalyticsSyncService;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.workout.dto.ExerciseSetData;
import project.fitnessapplicationexam.workout.dto.SetData;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.model.WorkoutSet;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;
import project.fitnessapplicationexam.workout.service.WorkoutService;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

	@Mock private WorkoutSessionRepository workoutSessionRepository;
	@Mock private WorkoutSetRepository workoutSetRepository;
	@Mock private ExerciseRepository exerciseRepository;
	@Mock private AnalyticsSyncService analyticsSyncService;
	@InjectMocks private WorkoutService workoutService;

	@Test
	void start_createsSession() {
		UUID user = UUID.randomUUID();
		when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		WorkoutSession s = workoutService.start(user);
		assertEquals(user, s.getUserId());
	}

	@Test
	void finishSession_marksFinishedWhenOwner() {
		UUID user = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		WorkoutSession s = new WorkoutSession();
		s.setId(id);
		s.setUserId(user);
		when(workoutSessionRepository.findById(id)).thenReturn(Optional.of(s));
		when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		when(workoutSetRepository.findAllBySessionId(id)).thenReturn(List.of());
		workoutService.finishSession(id, user);
		assertEquals(SessionStatus.FINISHED, s.getStatus());
		verify(workoutSessionRepository).save(s);
		verify(analyticsSyncService).syncWorkout(eq(s), anyList());
	}

	@Test
	void deleteSession_throwsWhenNotOwner() {
		UUID user = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		WorkoutSession s = new WorkoutSession();
		s.setId(id);
		s.setUserId(UUID.randomUUID());
		when(workoutSessionRepository.findById(id)).thenReturn(Optional.of(s));
		assertThrows(ResponseStatusException.class, () -> workoutService.deleteSession(id, user));
	}

	@Test
	void history_returnsSessions() {
		UUID userId = UUID.randomUUID();
		when(workoutSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = workoutService.history(userId);
		assertNotNull(result);
	}

	@Test
	void getRecentSessions_limit5_returnsTop5() {
		UUID userId = UUID.randomUUID();
		when(workoutSessionRepository.findTop5ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = workoutService.getRecentSessions(userId, 5);
		assertNotNull(result);
	}

	@Test
	void getRecentSessions_limit50_returnsTop50() {
		UUID userId = UUID.randomUUID();
		when(workoutSessionRepository.findTop50ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = workoutService.getRecentSessions(userId, 50);
		assertNotNull(result);
	}

	@Test
	void getRecentSessions_otherLimit_returnsAll() {
		UUID userId = UUID.randomUUID();
		when(workoutSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = workoutService.getRecentSessions(userId, 10);
		assertNotNull(result);
	}

	@Test
	void getSessionSets_returnsSets() {
		UUID sessionId = UUID.randomUUID();
		when(workoutSetRepository.findAllBySessionIdOrderByExerciseOrderAscIdAsc(sessionId)).thenReturn(List.of());

		List<WorkoutSet> result = workoutService.getSessionSets(sessionId);
		assertNotNull(result);
	}

	@Test
	void getAvailableExercises_returnsExercises() {
		UUID userId = UUID.randomUUID();
		when(exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(anyList())).thenReturn(List.of());

		List<Exercise> result = workoutService.getAvailableExercises(userId);
		assertNotNull(result);
	}

	@Test
	void getExercisesByIds_returnsMap() {
		UUID exerciseId = UUID.randomUUID();
		Exercise exercise = Exercise.builder().id(exerciseId).build();
		when(exerciseRepository.findAllById(anyList())).thenReturn(List.of(exercise));

		Map<UUID, Exercise> result = workoutService.getExercisesByIds(List.of(exerciseId));
		assertEquals(1, result.size());
	}

	@Test
	void findExerciseById_returnsExercise() {
		UUID exerciseId = UUID.randomUUID();
		when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.empty());

		Optional<Exercise> result = workoutService.findExerciseById(exerciseId);
		assertTrue(result.isEmpty());
	}

    @Test
    void finishSession_alreadyFinished_throwsBadRequest() {
        UUID user = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        WorkoutSession s = new WorkoutSession();
        s.setId(id);
        s.setUserId(user);
        s.setFinishedAt(LocalDateTime.now());
        when(workoutSessionRepository.findById(id)).thenReturn(Optional.of(s));

        assertThrows(ResponseStatusException.class, () -> workoutService.finishSession(id, user));
        verify(workoutSessionRepository, never()).save(any());
    }

	@Test
	void finishSessionWithSets_savesSets() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(userId);
		when(workoutSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
		when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		when(workoutSetRepository.findAllBySessionId(sessionId)).thenReturn(List.of());
		
		Exercise exercise = Exercise.builder()
				.id(exerciseId)
				.ownerUserId(userId)
				.name("Bench Press")
				.equipment(Equipment.BARBELL)
				.primaryMuscle(MuscleGroup.CHEST)
				.build();
		when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
		
		SetData setData = new SetData(100.0, 10, null, null, null, null);
		ExerciseSetData exData = new ExerciseSetData(exerciseId, List.of(setData));
		
		workoutService.finishSessionWithSets(sessionId, userId, List.of(exData));
		
		verify(workoutSetRepository).saveAll(anyList());
		assertEquals(SessionStatus.FINISHED, session.getStatus());
		verify(analyticsSyncService).syncWorkout(eq(session), anyList());
	}

	@Test
	void finishSessionWithSets_notFound_throwsException() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(workoutSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> 
				workoutService.finishSessionWithSets(sessionId, userId, List.of()));
	}

	@Test
	void finishSessionWithSets_wrongOwner_throwsException() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(otherUserId);
		when(workoutSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

		assertThrows(ResponseStatusException.class, () -> 
				workoutService.finishSessionWithSets(sessionId, userId, List.of()));
	}

	@Test
	void finishSessionWithSets_emptySets_marksFinished() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(userId);
		when(workoutSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
		when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		when(workoutSetRepository.findAllBySessionId(sessionId)).thenReturn(List.of());

		workoutService.finishSessionWithSets(sessionId, userId, List.of());
		assertEquals(SessionStatus.FINISHED, session.getStatus());
		verify(analyticsSyncService).syncWorkout(eq(session), anyList());
	}

	@Test
	void deleteSession_deletesSession() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(userId);
		when(workoutSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

		workoutService.deleteSession(sessionId, userId);
		verify(workoutSetRepository).deleteBySessionId(sessionId);
		verify(workoutSessionRepository).deleteById(sessionId);
		verify(analyticsSyncService).deleteWorkout(sessionId);
	}

	@Test
	void deleteSession_notFound_throwsException() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(workoutSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> workoutService.deleteSession(sessionId, userId));
	}
}

