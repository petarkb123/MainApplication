package project.fitnessapplication.exercise.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import project.fitnessapplication.exercise.model.Equipment;
import project.fitnessapplication.exercise.model.MuscleGroup;

@Data
public class ExerciseForm {
    @NotBlank(message = "Exercise name is required")
    private String name;
    
    private MuscleGroup muscleGroup = MuscleGroup.OTHER;
    private Equipment equipment = Equipment.OTHER;
}
