package project.fitnessapplicationexam.workout.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplicationexam.config.SystemDefault;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.workout.dto.ExerciseSetData;
import project.fitnessapplicationexam.workout.dto.SetData;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.model.WorkoutSet;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;
import project.fitnessapplicationexam.analytics.AnalyticsSyncService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import project.fitnessapplicationexam.config.ValidationConstants;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutService.class);

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final ExerciseRepository exerciseRepository;
    private final AnalyticsSyncService analyticsSyncService;

    @Transactional(readOnly = true)
    @Cacheable(value = "workoutSessions", key = "#user")
    public List<WorkoutSession> history(UUID user) {
        return workoutSessionRepository.findAllByUserIdOrderByStartedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<WorkoutSession> getRecentSessions(UUID userId, int limit) {
        if (limit == ValidationConstants.RECENT_SESSIONS_LIMIT_5) {
            return workoutSessionRepository.findTop5ByUserIdOrderByStartedAtDesc(userId);
        } else if (limit == ValidationConstants.RECENT_SESSIONS_LIMIT_50) {
            return workoutSessionRepository.findTop50ByUserIdOrderByStartedAtDesc(userId);
        }
        return workoutSessionRepository.findAllByUserIdOrderByStartedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> findById(UUID sessionId) {
        return workoutSessionRepository.findById(sessionId);
    }

    @Transactional(readOnly = true)
    public List<WorkoutSet> getSessionSets(UUID sessionId) {
        return workoutSetRepository.findAllBySessionIdOrderByExerciseOrderAscIdAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<Exercise> getAvailableExercises(UUID userId) {
        List<UUID> owners = List.of(SystemDefault.SYSTEM_USER_ID, userId);
        return exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(owners);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Exercise> getExercisesByIds(List<UUID> exerciseIds) {
        return exerciseRepository.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, exercise -> exercise));
    }

    @Transactional(readOnly = true)
    public Optional<Exercise> findExerciseById(UUID exerciseId) {
        return exerciseRepository.findById(exerciseId);
    }

    @Transactional
    public WorkoutSession start(UUID user) {
        WorkoutSession session = new WorkoutSession();
        session.setUserId(user);
        session.setStartedAt(LocalDateTime.now());
        WorkoutSession saved = workoutSessionRepository.save(session);
        log.info("Workout session started for user {}: {}", user, saved.getId());
        return saved;
    }

    @Transactional
    public void finishSession(UUID sessionId, UUID userId) {
        WorkoutSession session = workoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        
        if (session.getFinishedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session already finished");
        }
        
        session.setFinishedAt(LocalDateTime.now());
        session.setStatus(SessionStatus.FINISHED);
        workoutSessionRepository.save(session);
        List<WorkoutSet> syncedSets = workoutSetRepository.findAllBySessionId(sessionId);
        analyticsSyncService.syncWorkout(session, syncedSets);
        log.info("Workout session {} finished for user {}", sessionId, userId);
    }
    
    @Transactional
    @CacheEvict(value = {"workoutSessions", "weeklyStats"}, allEntries = true)
    public void finishSessionWithFallback(UUID sessionId, UUID userId) {
        try {
            finishSessionWithSets(sessionId, userId, null);
        } catch (Exception e) {
            log.debug("Fallback to simple finish for session {}", sessionId);
            finishSession(sessionId, userId);
        }
    }
    
    @Transactional
    @CacheEvict(value = {"workoutSessions", "weeklyStats"}, allEntries = true)
    public void finishSessionWithSets(UUID sessionId, UUID userId, List<ExerciseSetData> exerciseSets) {
        WorkoutSession session = workoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }

        if (exerciseSets != null && !exerciseSets.isEmpty()) {
            workoutSetRepository.deleteBySessionId(sessionId);
            List<WorkoutSet> toSave = new ArrayList<>();

            int exerciseOrderIndex = 0;
            for (ExerciseSetData exData : exerciseSets) {
                if (exData == null || exData.exerciseId() == null) {
                    continue;
                }

                Exercise exercise = exerciseRepository.findById(exData.exerciseId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Exercise not found: " + exData.exerciseId()));

                boolean ownerOk = Objects.equals(exercise.getOwnerUserId(), userId) ||
                        Objects.equals(exercise.getOwnerUserId(), SystemDefault.SYSTEM_USER_ID);
                if (!ownerOk) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise not accessible");
                }

                if (exData.sets() != null && !exData.sets().isEmpty()) {
                    UUID exerciseId = exercise.getId();
                    for (SetData setData : exData.sets()) {
                        if (setData == null) {
                            continue;
                        }

                        int reps = (setData.reps() == null) ? 0 : Math.max(0, setData.reps());
                        double weight = (setData.weight() == null) ? 0.0 : Math.max(0.0, setData.weight());
                        if (reps <= 0 || weight <= 0.0) {
                            continue;
                        }

                        toSave.add(WorkoutSet.builder()
                                .sessionId(sessionId)
                                .exerciseId(exerciseId)
                                .reps(reps)
                                .weight(BigDecimal.valueOf(weight))
                                .groupId(setData.groupId())
                                .groupType(setData.groupType())
                                .groupOrder(setData.groupOrder())
                                .setNumber(setData.setNumber())
                                .exerciseOrder(exerciseOrderIndex)
                                .build());
                    }
                    exerciseOrderIndex++;
                }
            }
            if (!toSave.isEmpty()) {
                workoutSetRepository.saveAll(toSave);
            }
        }

        if (session.getFinishedAt() == null) {
            session.setFinishedAt(LocalDateTime.now());
            session.setStatus(SessionStatus.FINISHED);
            workoutSessionRepository.save(session);
        }

        List<WorkoutSet> syncedSets = workoutSetRepository.findAllBySessionId(sessionId);
        analyticsSyncService.syncWorkout(session, syncedSets);
        log.info("Workout session {} finished for user {} with {} sets", sessionId, userId, syncedSets.size());
    }

    @Transactional
    @CacheEvict(value = {"workoutSessions", "weeklyStats"}, allEntries = true)
    public void deleteSession(UUID sessionId, UUID userId) {
        WorkoutSession session = workoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
        }

        workoutSetRepository.deleteBySessionId(sessionId);
        workoutSessionRepository.deleteById(sessionId);
        analyticsSyncService.deleteWorkout(sessionId);
        log.info("Workout session {} deleted for user {}", sessionId, userId);
    }
}
