package project.fitnessapplicationexam.workout.service;

import lombok.RequiredArgsConstructor;
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
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.model.WorkoutSet;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;
import project.fitnessapplicationexam.analytics.AnalyticsSyncService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutSetRepository setRepo;
    private final ExerciseRepository exerciseRepo;
    private final AnalyticsSyncService analyticsSyncService;

    @Transactional(readOnly = true)
    @Cacheable(value = "workoutSessions", key = "#user")
    public List<WorkoutSession> history(UUID user) {
        return sessionRepo.findAllByUserIdOrderByStartedAtDesc(user);
    }
    
    @Transactional(readOnly = true)
    public List<WorkoutSession> getRecentSessions(UUID userId, int limit) {
        if (limit == 5) {
            return sessionRepo.findTop5ByUserIdOrderByStartedAtDesc(userId);
        } else if (limit == 50) {
            return sessionRepo.findTop50ByUserIdOrderByStartedAtDesc(userId);
        }
        return sessionRepo.findAllByUserIdOrderByStartedAtDesc(userId);
    }
    
    @Transactional(readOnly = true)
    public Optional<WorkoutSession> findById(UUID sessionId) {
        return sessionRepo.findById(sessionId);
    }
    
    @Transactional(readOnly = true)
    public List<WorkoutSet> getSessionSets(UUID sessionId) {
        return setRepo.findAllBySessionIdOrderByExerciseOrderAscIdAsc(sessionId);
    }
    
    @Transactional(readOnly = true)
    public List<Exercise> getAvailableExercises(UUID userId) {
        List<UUID> owners = List.of(SystemDefault.SYSTEM_USER_ID, userId);
        return exerciseRepo.findAllByOwnerUserIdInOrderByNameAsc(owners);
    }
    
    @Transactional(readOnly = true)
    public Map<UUID, Exercise> getExercisesByIds(List<UUID> exerciseIds) {
        return exerciseRepo.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));
    }
    
    @Transactional(readOnly = true)
    public Optional<Exercise> findExerciseById(UUID exerciseId) {
        return exerciseRepo.findById(exerciseId);
    }

    @Transactional
    public WorkoutSession start(UUID user) {
        WorkoutSession session = new WorkoutSession();
        session.setUserId(user);
        session.setStartedAt(LocalDateTime.now());
        return sessionRepo.save(session);
    }
    
    @Transactional
    public void finishSession(UUID sessionId, UUID userId) {
        sessionRepo.findById(sessionId).ifPresent(s -> {
            if (Objects.equals(s.getUserId(), userId) && s.getFinishedAt() == null) {
                s.setFinishedAt(LocalDateTime.now());
                s.setStatus(SessionStatus.FINISHED);
                sessionRepo.save(s);
                List<WorkoutSet> syncedSets = setRepo.findAllBySessionId(sessionId);
                analyticsSyncService.syncWorkout(s, syncedSets);
            }
        });
    }
    
    @Transactional
    @CacheEvict(value = {"workoutSessions", "weeklyStats"}, allEntries = true)
    public void finishSessionWithSets(UUID sessionId, UUID userId, List<ExerciseSetData> exerciseSets) {
        WorkoutSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        
        if (exerciseSets != null && !exerciseSets.isEmpty()) {
            setRepo.deleteBySessionId(sessionId);
            ArrayList<WorkoutSet> toSave = new ArrayList<>();
            
            int exerciseOrderIndex = 0;
            for (project.fitnessapplicationexam.workout.dto.ExerciseSetData exData : exerciseSets) {
                if (exData == null || exData.exerciseId() == null) {
                    continue;
                }
                
                Exercise exercise = exerciseRepo.findById(exData.exerciseId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise not found: " + exData.exerciseId()));
                
                if (exercise == null || exercise.getId() == null) {
                    continue;
                }
                
                boolean ownerOk = Objects.equals(exercise.getOwnerUserId(), userId) ||
                        Objects.equals(exercise.getOwnerUserId(), SystemDefault.SYSTEM_USER_ID);
                if (!ownerOk) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise not accessible");
                }
                
                if (exData.sets() != null && !exData.sets().isEmpty()) {
                    UUID exerciseIdValue = exercise.getId();
                    if (exerciseIdValue == null) {
                        exerciseOrderIndex++;
                        continue;
                    }
                    for (project.fitnessapplicationexam.workout.dto.SetData setData : exData.sets()) {
                        if (setData == null) continue;
                        
                        int reps = (setData.reps() == null) ? 0 : Math.max(0, setData.reps());
                        double weight = (setData.weight() == null) ? 0.0 : Math.max(0.0, setData.weight());
                        if (reps <= 0 || weight <= 0.0) continue;
                        
                        if (exerciseIdValue == null) {
                            continue;
                        }
                        
                        toSave.add(WorkoutSet.builder()
                                .sessionId(sessionId)
                                .exerciseId(exerciseIdValue)
                                .reps(reps)
                                .weight(java.math.BigDecimal.valueOf(weight))
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
                setRepo.saveAll(toSave);
            }
        }
        
        if (session.getFinishedAt() == null) {
            session.setFinishedAt(LocalDateTime.now());
            session.setStatus(SessionStatus.FINISHED);
            session = sessionRepo.save(session);
        } else {
            session = sessionRepo.findById(sessionId).orElse(session);
        }

        List<WorkoutSet> syncedSets = setRepo.findAllBySessionId(sessionId);
        analyticsSyncService.syncWorkout(session, syncedSets);
    }
    
    @Transactional
    @CacheEvict(value = {"workoutSessions", "weeklyStats"}, allEntries = true)
    public void deleteSession(UUID sessionId, UUID userId) {
        Optional<WorkoutSession> session = sessionRepo.findById(sessionId);
        if (session.isEmpty() || !Objects.equals(session.get().getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
        }
        
        setRepo.deleteBySessionId(sessionId);
        sessionRepo.deleteById(sessionId);
        analyticsSyncService.deleteWorkout(sessionId);
    }
}
