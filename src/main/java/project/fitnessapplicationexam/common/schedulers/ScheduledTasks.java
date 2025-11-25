package project.fitnessapplicationexam.common.schedulers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutSetRepository workoutSetRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupAbandonedWorkoutSessions() {
        log.info("Starting cleanup of abandoned workout sessions");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        
        List<WorkoutSession> abandonedSessions = workoutSessionRepository.findAll()
                .stream()
                .filter(session -> session.getStatus() == SessionStatus.IN_PROGRESS)
                .filter(session -> session.getStartedAt().isBefore(cutoffDate))
                .filter(session -> session.getFinishedAt() == null)
                .toList();
        
        abandonedSessions.forEach(session -> {
            log.info("Cleaning up abandoned session {} for user {} started on {}", 
                    session.getId(), session.getUserId(), session.getStartedAt());
            workoutSetRepository.deleteBySessionId(session.getId());
            workoutSessionRepository.delete(session);
        });
        
        log.info("Cleanup completed: {} abandoned sessions removed", abandonedSessions.size());
    }
}

