package project.fitnessapplicationexam.exercise.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.config.SystemDefault;
import java.util.UUID;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;

import java.time.LocalDateTime;
import project.fitnessapplicationexam.analytics.AnalyticsSyncService;

@Component
@RequiredArgsConstructor
public class ExerciseBuiltinSeeder implements ApplicationRunner {

    private final ExerciseRepository exerciseRepository;
    private final AnalyticsSyncService analyticsSyncService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        final UUID SYS = SystemDefault.SYSTEM_USER_ID;

        
        upsert("Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, SYS);
        upsert("Dumbbell Bench Press", MuscleGroup.CHEST, Equipment.DUMBBELL, SYS);
        upsert("Incline Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, SYS);
        upsert("Decline Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, SYS);
        upsert("Push-Ups", MuscleGroup.CHEST, Equipment.BODYWEIGHT, SYS);
        upsert("Chest Dips", MuscleGroup.CHEST, Equipment.BODYWEIGHT, SYS);

        
        upsert("Pull-Ups", MuscleGroup.BACK, Equipment.BODYWEIGHT, SYS);
        upsert("Barbell Rows", MuscleGroup.BACK, Equipment.BARBELL, SYS);
        upsert("Dumbbell Rows", MuscleGroup.BACK, Equipment.DUMBBELL, SYS);
        upsert("Lat Pulldowns", MuscleGroup.BACK, Equipment.CABLE, SYS);
        upsert("T-Bar Rows", MuscleGroup.BACK, Equipment.BARBELL, SYS);
        upsert("Deadlifts", MuscleGroup.BACK, Equipment.BARBELL, SYS);
        upsert("Straight-Arm Pulldowns", MuscleGroup.BACK, Equipment.CABLE, SYS);

        
        upsert("Barbell Curl", MuscleGroup.BICEPS, Equipment.BARBELL, SYS);
        upsert("Dumbbell Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, SYS);
        upsert("Preacher Curl", MuscleGroup.BICEPS, Equipment.BARBELL, SYS);
        upsert("Hammer Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, SYS);
        upsert("Concentration Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, SYS);
        upsert("Cable Curl", MuscleGroup.BICEPS, Equipment.CABLE, SYS);

        
        upsert("Close-Grip Bench Press", MuscleGroup.TRICEPS, Equipment.BARBELL, SYS);
        upsert("Tricep Dips", MuscleGroup.TRICEPS, Equipment.BODYWEIGHT, SYS);
        upsert("Overhead Tricep Extension", MuscleGroup.TRICEPS, Equipment.DUMBBELL, SYS);
        upsert("Skull Crushers", MuscleGroup.TRICEPS, Equipment.BARBELL, SYS);
        upsert("Tricep Pushdowns", MuscleGroup.TRICEPS, Equipment.CABLE, SYS);
        upsert("Dumbbell Kickbacks", MuscleGroup.TRICEPS, Equipment.DUMBBELL, SYS);

        
        upsert("Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, SYS);
        upsert("Arnold Press", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, SYS);
        upsert("Lateral Raises", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, SYS);
        upsert("Front Raises", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, SYS);
        upsert("Rear Delt Flyes", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, SYS);
        upsert("Cable Lateral Raises", MuscleGroup.SHOULDERS, Equipment.CABLE, SYS);
        upsert("Upright Rows", MuscleGroup.SHOULDERS, Equipment.BARBELL, SYS);

        
        upsert("Wrist Curls", MuscleGroup.FOREARMS, Equipment.BARBELL, SYS);
        upsert("Reverse Wrist Curls", MuscleGroup.FOREARMS, Equipment.BARBELL, SYS);
        upsert("Farmer’s Carry", MuscleGroup.FOREARMS, Equipment.DUMBBELL, SYS);
        upsert("Plate Pinches", MuscleGroup.FOREARMS, Equipment.OTHER, SYS);
        upsert("Towel Pull-Ups", MuscleGroup.FOREARMS, Equipment.BODYWEIGHT, SYS);

        
        upsert("Plank", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);
        upsert("Hanging Leg Raises", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);
        upsert("Cable Crunches", MuscleGroup.CORE, Equipment.CABLE, SYS);
        upsert("Russian Twists", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);
        upsert("Bicycle Crunches", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);
        upsert("Ab Rollouts", MuscleGroup.CORE, Equipment.OTHER, SYS);
        upsert("Sit-Ups", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);
        upsert("V-Ups", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);
        upsert("Mountain Climbers", MuscleGroup.CORE, Equipment.BODYWEIGHT, SYS);

        
        upsert("Back Squat", MuscleGroup.LEGS, Equipment.BARBELL, SYS);
        upsert("Front Squat", MuscleGroup.LEGS, Equipment.BARBELL, SYS);
        upsert("Leg Extension", MuscleGroup.LEGS, Equipment.MACHINE, SYS);
        upsert("Bulgarian Split Squat", MuscleGroup.LEGS, Equipment.DUMBBELL, SYS);
        upsert("Walking Lunges", MuscleGroup.LEGS, Equipment.DUMBBELL, SYS);
        upsert("Step-Ups", MuscleGroup.LEGS, Equipment.DUMBBELL, SYS);
        upsert("Sissy Squats", MuscleGroup.LEGS, Equipment.OTHER, SYS);

        
        upsert("Romanian Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, SYS);
        upsert("Stiff-Leg Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, SYS);
        upsert("Good Mornings", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, SYS);
        upsert("Glute-Ham Raise", MuscleGroup.HAMSTRINGS, Equipment.MACHINE, SYS);
        upsert("Seated Leg Curl", MuscleGroup.HAMSTRINGS, Equipment.MACHINE, SYS);
        upsert("Lying Leg Curl", MuscleGroup.HAMSTRINGS, Equipment.MACHINE, SYS);

        
        upsert("Standing Calf Raises", MuscleGroup.CALVES, Equipment.MACHINE, SYS);
        upsert("Seated Calf Raises", MuscleGroup.CALVES, Equipment.MACHINE, SYS);
        upsert("Donkey Calf Raises", MuscleGroup.CALVES, Equipment.MACHINE, SYS);
        upsert("Single-Leg Calf Raises", MuscleGroup.CALVES, Equipment.BODYWEIGHT, SYS);
        upsert("Leg Press Calf Press", MuscleGroup.CALVES, Equipment.MACHINE, SYS);
    }

    private void upsert(String name, MuscleGroup muscleGroup, Equipment equipment, UUID owner) {
        exerciseRepository.findByNameIgnoreCaseAndOwnerUserId(name, owner).ifPresentOrElse(
                existing -> {
                    // Exercise already exists, no action needed
                },
                () -> {
                    Exercise exercise = Exercise.builder()
                            .ownerUserId(owner)
                            .name(name)
                            .primaryMuscle(muscleGroup)
                            .equipment(equipment)
                            .createdOn(LocalDateTime.now())
                            .build();
                    Exercise saved = exerciseRepository.save(exercise);
                    analyticsSyncService.syncExercise(saved);
                }
        );
    }
}
