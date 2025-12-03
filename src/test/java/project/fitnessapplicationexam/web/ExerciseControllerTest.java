package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.exercise.service.ExerciseService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSettingsService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(ExerciseController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class ExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private ExerciseService exerciseService;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @SuppressWarnings("removal")
    @MockBean
    private ExerciseRepository exerciseRepository;

    @SuppressWarnings("removal")
    @MockBean
    private UserSettingsService userSettingsService;

    @Test
    void list_returnsExerciseListPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");
        user.setRole(UserRole.USER);

        Exercise exercise = new Exercise();
        exercise.setId(UUID.randomUUID());
        exercise.setName("Bench Press");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(anyList())).thenReturn(List.of(exercise));

        mockMvc.perform(get("/exercises"))
                .andExpect(status().isOk())
                .andExpect(view().name("exercises"));
    }

    @Test
    void create_exercise_redirectsToList() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Exercise exercise = new Exercise();
        exercise.setName("Squat");
        exercise.setPrimaryMuscle(MuscleGroup.LEGS);
        exercise.setEquipment(Equipment.BARBELL);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(exerciseService.create(any(Exercise.class))).thenReturn(exercise);

        mockMvc.perform(post("/exercises/create")
                .param("name", "Squat")
                .param("muscleGroup", "LEGS")
                .param("equipment", "BARBELL")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void delete_exercise_redirectsToList() throws Exception {
        UUID exerciseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Exercise exercise = new Exercise();
        exercise.setId(exerciseId);
        exercise.setOwnerUserId(userId);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(exerciseRepository.findByIdAndOwnerUserId(exerciseId, userId)).thenReturn(Optional.of(exercise));

        mockMvc.perform(post("/exercises/{id}/delete", exerciseId)
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

}

