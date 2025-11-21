package project.fitnessapplicationexam.exercise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.fitnessapplicationexam.analytics.AnalyticsSyncService;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.exercise.service.ExerciseService;
import project.fitnessapplicationexam.template.repository.TemplateItemRepository;
import project.fitnessapplicationexam.workout.repository.WorkoutSetRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;
    
    @Mock
    private TemplateItemRepository templateItemRepository;
    
    @Mock
    private WorkoutSetRepository workoutSetRepository;

    @Mock
    private AnalyticsSyncService analyticsSyncService;

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    void testCreateExercise() {
        Exercise exercise = Exercise.builder()
                .name("Bench Press")
                .primaryMuscle(MuscleGroup.CHEST)
                .equipment(Equipment.BARBELL)
                .ownerUserId(UUID.randomUUID())
                .createdOn(LocalDateTime.now())
                .build();
        
        when(exerciseRepository.save(any(Exercise.class))).thenReturn(exercise);
        
        Exercise result = exerciseService.create(exercise);
        
        assertNotNull(result);
        assertEquals("Bench Press", result.getName());
        verify(exerciseRepository, times(1)).save(exercise);
        verify(analyticsSyncService, times(1)).syncExercise(exercise);
    }

    @Test
    void testGetExercise() {
        UUID exerciseId = UUID.randomUUID();
        Exercise exercise = Exercise.builder()
                .id(exerciseId)
                .name("Squat")
                .primaryMuscle(MuscleGroup.LEGS)
                .build();
        
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        
        Exercise result = exerciseService.get(exerciseId);

        assertNotNull(result);
        assertEquals("Squat", result.getName());
    }

    @Test
    void testDeleteExercise() {
        UUID exerciseId = UUID.randomUUID();
        exerciseService.delete(exerciseId);

        verify(templateItemRepository, times(1)).deleteByExerciseId(exerciseId);
        verify(workoutSetRepository, times(1)).deleteByExerciseId(exerciseId);
        verify(exerciseRepository, times(1)).deleteById(exerciseId);
        verify(analyticsSyncService, times(1)).deleteExercise(exerciseId);
    }

    @Test
    void testByOwner() {
        UUID ownerId = UUID.randomUUID();
        List<Exercise> exercises = Arrays.asList(
                Exercise.builder().name("Exercise 1").build(),
                Exercise.builder().name("Exercise 2").build()
        );
        
        when(exerciseRepository.findAllByOwnerUserId(ownerId)).thenReturn(exercises);

        List<Exercise> result = exerciseService.byOwner(ownerId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(exerciseRepository, times(1)).findAllByOwnerUserId(ownerId);
    }
}

