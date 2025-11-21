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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService {
    private final ExerciseRepository repo;
    private final TemplateItemRepository templateItemRepo;
    private final WorkoutSetRepository workoutSetRepo;
    private final AnalyticsSyncService analyticsSyncService;
    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    @Cacheable(value = "exercises", key = "#owner")
    public List<Exercise> byOwner(UUID owner){ 
        log.debug("Fetching exercises for owner {}", owner);
        return repo.findAllByOwnerUserId(owner); 
    }

    @Transactional 
    @CacheEvict(value = "exercises", key = "#e.ownerUserId")
    public Exercise create(Exercise e){
        log.info("Creating exercise '{}' for user {}", e.getName(), e.getOwnerUserId());
        Exercise saved = repo.save(e);
        analyticsSyncService.syncExercise(saved);
        return saved;
    }

    @Cacheable(value = "exercise", key = "#id")
    public Exercise get(UUID id){ 
        log.debug("Fetching exercise {}", id);
        return repo.findById(id).orElseThrow(); 
    }

    @Transactional 
    @CacheEvict(value = {"exercises", "exercise", "templates"}, allEntries = true)
    public void delete(UUID id){
        log.warn("Deleting exercise {} and related records", id);
        templateItemRepo.deleteByExerciseId(id);
        workoutSetRepo.deleteByExerciseId(id);
        repo.deleteById(id);
        analyticsSyncService.deleteExercise(id);
    }


}
