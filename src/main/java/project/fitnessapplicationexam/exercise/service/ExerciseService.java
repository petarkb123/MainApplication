package project.fitnessapplicationexam.exercise.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.template.repository.TemplateItemRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;
import project.fitnessapplicationexam.analytics.AnalyticsSyncService;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);
    private final ExerciseRepository exerciseRepository;
    private final TemplateItemRepository templateItemRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final AnalyticsSyncService analyticsSyncService;

    @Cacheable(value = "exercises", key = "#owner")
    public List<Exercise> byOwner(UUID owner) {
        log.debug("Fetching exercises for owner {}", owner);
        return exerciseRepository.findAllByOwnerUserId(owner);
    }

    @Transactional
    @CacheEvict(value = "exercises", key = "#exercise.ownerUserId")
    public Exercise create(Exercise exercise) {
        log.info("Creating exercise '{}' for user {}", exercise.getName(), exercise.getOwnerUserId());
        Exercise saved = exerciseRepository.save(exercise);
        analyticsSyncService.syncExercise(saved);
        return saved;
    }

    @Cacheable(value = "exercise", key = "#id")
    public Exercise get(UUID id) {
        log.debug("Fetching exercise {}", id);
        return exerciseRepository.findById(id).orElseThrow();
    }

    public List<Exercise> findAllByOwnerUserIdInOrderByNameAsc(List<UUID> ownerIds) {
        return exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(ownerIds);
    }

    public Optional<Exercise> findByIdAndOwnerUserId(UUID id, UUID ownerId) {
        return exerciseRepository.findByIdAndOwnerUserId(id, ownerId);
    }

    @Transactional
    @CacheEvict(value = {"exercises", "exercise", "templates"}, allEntries = true)
    public void delete(UUID id) {
        log.warn("Deleting exercise {} and related records", id);
        templateItemRepository.deleteByExerciseId(id);
        workoutSetRepository.deleteByExerciseId(id);
        exerciseRepository.deleteById(id);
        analyticsSyncService.deleteExercise(id);
    }
}
