package project.fitnessapplication.workout.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplication.config.SystemDefault;
import project.fitnessapplication.exercise.model.Exercise;
import project.fitnessapplication.exercise.repository.ExerciseRepository;
import project.fitnessapplication.workout.dto.ExerciseSetData;
import project.fitnessapplication.workout.model.SessionStatus;
import project.fitnessapplication.workout.model.WorkoutSession;
import project.fitnessapplication.workout.model.WorkoutSet;
import project.fitnessapplication.workout.repository.WorkoutSessionRepository;
import project.fitnessapplication.workout.repository.WorkoutSetRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutSessionRepository sessionRepo;
    private final WorkoutSetRepository setRepo;
    private final ExerciseRepository exerciseRepo;

    @Transactional(readOnly = true)
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
        var owners = List.of(SystemDefault.SYSTEM_USER_ID, userId);
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
        var session = new WorkoutSession();
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
            }
        });
    }
    
    @Transactional
    public void finishSessionWithSets(UUID sessionId, UUID userId, List<ExerciseSetData> exerciseSets) {
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        
        if (exerciseSets != null && !exerciseSets.isEmpty()) {
            setRepo.deleteBySessionId(sessionId);
            var toSave = new ArrayList<WorkoutSet>();
            
            int exerciseOrderIndex = 0;
            for (var exData : exerciseSets) {
                var exercise = exerciseRepo.findById(exData.exerciseId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise not found"));
                
                boolean ownerOk = Objects.equals(exercise.getOwnerUserId(), userId) ||
                        Objects.equals(exercise.getOwnerUserId(), SystemDefault.SYSTEM_USER_ID);
                if (!ownerOk) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise not accessible");
                }
                
                if (exData.sets() != null) {
                    for (var setData : exData.sets()) {
                        int reps = (setData.reps() == null) ? 0 : Math.max(0, setData.reps());
                        double weight = (setData.weight() == null) ? 0.0 : Math.max(0.0, setData.weight());
                        if (reps <= 0 || weight <= 0.0) continue;
                        
                        toSave.add(WorkoutSet.builder()
                                .sessionId(sessionId)
                                .exerciseId(exercise.getId())
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
            sessionRepo.save(session);
        }
    }
    
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        var session = sessionRepo.findById(sessionId);
        if (session.isEmpty() || !Objects.equals(session.get().getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
        }
        
        setRepo.deleteBySessionId(sessionId);
        sessionRepo.deleteById(sessionId);
    }
}
