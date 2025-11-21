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

	@Mock private WorkoutSessionRepository sessionRepo;
	@Mock private WorkoutSetRepository setRepo;
	@Mock private ExerciseRepository exerciseRepo;
	@Mock private AnalyticsSyncService analyticsSyncService;
	@InjectMocks private WorkoutService service;

	@Test
	void start_createsSession() {
		UUID user = UUID.randomUUID();
		when(sessionRepo.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		WorkoutSession s = service.start(user);
		assertEquals(user, s.getUserId());
	}

	@Test
	void finishSession_marksFinishedWhenOwner() {
		UUID user = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		WorkoutSession s = new WorkoutSession();
		s.setId(id);
		s.setUserId(user);
		when(sessionRepo.findById(id)).thenReturn(Optional.of(s));
		when(sessionRepo.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		when(setRepo.findAllBySessionId(id)).thenReturn(List.of());
		service.finishSession(id, user);
		assertEquals(SessionStatus.FINISHED, s.getStatus());
		verify(sessionRepo).save(s);
		verify(analyticsSyncService).syncWorkout(eq(s), anyList());
	}

	@Test
	void deleteSession_throwsWhenNotOwner() {
		UUID user = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		WorkoutSession s = new WorkoutSession();
		s.setId(id);
		s.setUserId(UUID.randomUUID());
		when(sessionRepo.findById(id)).thenReturn(Optional.of(s));
		assertThrows(ResponseStatusException.class, () -> service.deleteSession(id, user));
	}

	@Test
	void history_returnsSessions() {
		UUID userId = UUID.randomUUID();
		when(sessionRepo.findAllByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = service.history(userId);
		assertNotNull(result);
	}

	@Test
	void getRecentSessions_limit5_returnsTop5() {
		UUID userId = UUID.randomUUID();
		when(sessionRepo.findTop5ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = service.getRecentSessions(userId, 5);
		assertNotNull(result);
	}

	@Test
	void getRecentSessions_limit50_returnsTop50() {
		UUID userId = UUID.randomUUID();
		when(sessionRepo.findTop50ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = service.getRecentSessions(userId, 50);
		assertNotNull(result);
	}

	@Test
	void getRecentSessions_otherLimit_returnsAll() {
		UUID userId = UUID.randomUUID();
		when(sessionRepo.findAllByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<WorkoutSession> result = service.getRecentSessions(userId, 10);
		assertNotNull(result);
	}

	@Test
	void getSessionSets_returnsSets() {
		UUID sessionId = UUID.randomUUID();
		when(setRepo.findAllBySessionIdOrderByExerciseOrderAscIdAsc(sessionId)).thenReturn(List.of());

		List<WorkoutSet> result = service.getSessionSets(sessionId);
		assertNotNull(result);
	}

	@Test
	void getAvailableExercises_returnsExercises() {
		UUID userId = UUID.randomUUID();
		when(exerciseRepo.findAllByOwnerUserIdInOrderByNameAsc(anyList())).thenReturn(List.of());

		List<Exercise> result = service.getAvailableExercises(userId);
		assertNotNull(result);
	}

	@Test
	void getExercisesByIds_returnsMap() {
		UUID exerciseId = UUID.randomUUID();
		Exercise exercise = Exercise.builder().id(exerciseId).build();
		when(exerciseRepo.findAllById(anyList())).thenReturn(List.of(exercise));

		Map<UUID, Exercise> result = service.getExercisesByIds(List.of(exerciseId));
		assertEquals(1, result.size());
	}

	@Test
	void findExerciseById_returnsExercise() {
		UUID exerciseId = UUID.randomUUID();
		when(exerciseRepo.findById(exerciseId)).thenReturn(Optional.empty());

		Optional<Exercise> result = service.findExerciseById(exerciseId);
		assertTrue(result.isEmpty());
	}

	@Test
	void finishSession_alreadyFinished_skips() {
		UUID user = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		WorkoutSession s = new WorkoutSession();
		s.setId(id);
		s.setUserId(user);
		s.setFinishedAt(LocalDateTime.now());
		when(sessionRepo.findById(id)).thenReturn(Optional.of(s));

		service.finishSession(id, user);
		verify(sessionRepo, never()).save(any());
	}

	@Test
	void finishSessionWithSets_savesSets() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID exerciseId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(userId);
		when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
		when(sessionRepo.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		when(setRepo.findAllBySessionId(sessionId)).thenReturn(List.of());
		
		Exercise exercise = Exercise.builder()
				.id(exerciseId)
				.ownerUserId(userId)
				.name("Bench Press")
				.equipment(Equipment.BARBELL)
				.primaryMuscle(MuscleGroup.CHEST)
				.build();
		when(exerciseRepo.findById(exerciseId)).thenReturn(Optional.of(exercise));
		
		SetData setData = new SetData(100.0, 10, null, null, null, null);
		ExerciseSetData exData = new ExerciseSetData(exerciseId, List.of(setData));
		
		service.finishSessionWithSets(sessionId, userId, List.of(exData));
		
		verify(setRepo).saveAll(anyList());
		assertEquals(SessionStatus.FINISHED, session.getStatus());
		verify(analyticsSyncService).syncWorkout(eq(session), anyList());
	}

	@Test
	void finishSessionWithSets_notFound_throwsException() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(sessionRepo.findById(sessionId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> 
				service.finishSessionWithSets(sessionId, userId, List.of()));
	}

	@Test
	void finishSessionWithSets_wrongOwner_throwsException() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(otherUserId);
		when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

		assertThrows(ResponseStatusException.class, () -> 
				service.finishSessionWithSets(sessionId, userId, List.of()));
	}

	@Test
	void finishSessionWithSets_emptySets_marksFinished() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		
		WorkoutSession session = new WorkoutSession();
		session.setId(sessionId);
		session.setUserId(userId);
		when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
		when(sessionRepo.save(any(WorkoutSession.class))).thenAnswer(inv -> inv.getArgument(0));
		when(setRepo.findAllBySessionId(sessionId)).thenReturn(List.of());

		service.finishSessionWithSets(sessionId, userId, List.of());
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
		when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

		service.deleteSession(sessionId, userId);
		verify(setRepo).deleteBySessionId(sessionId);
		verify(sessionRepo).deleteById(sessionId);
		verify(analyticsSyncService).deleteWorkout(sessionId);
	}

	@Test
	void deleteSession_notFound_throwsException() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(sessionRepo.findById(sessionId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> service.deleteSession(sessionId, userId));
	}
}

