package project.fitnessapplicationexam.exercise.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;

@Data
public class ExerciseForm {
    @NotBlank(message = "Exercise name is required")
    private String name;
    
    private MuscleGroup muscleGroup = MuscleGroup.OTHER;
    private Equipment equipment = Equipment.OTHER;
}
