package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.template.model.TemplateItem;
import project.fitnessapplicationexam.template.model.WorkoutTemplate;
import project.fitnessapplicationexam.template.service.TemplateService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.workout.service.WorkoutService;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemplateController.class)
@WithMockUser
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @SuppressWarnings("removal")
    @MockBean
    private TemplateService templateService;

    @SuppressWarnings("removal")
    @MockBean
    private WorkoutService workoutService;

    @SuppressWarnings("removal")
    @MockBean
    private ExerciseRepository exerciseRepository;

    @Test
    void listTemplates_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.list(any())).thenReturn(List.of());
        when(templateService.getAvailableExercises(any())).thenReturn(List.of());

        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk());
    }

    @Test
    void createForm_returnsCreatePage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.getAvailableExercises(any())).thenReturn(List.of());

        mockMvc.perform(get("/templates/create"))
                .andExpect(status().isOk());
    }

    @Test
    void editForm_returnsEditPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(true);

        WorkoutTemplate template = new WorkoutTemplate();
        template.setId(templateId);
        template.setOwnerUserId(userId);
        template.setName("Test Template");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.findByIdAndOwner(templateId, userId)).thenReturn(Optional.of(template));
        when(templateService.getTemplateItems(templateId)).thenReturn(List.of());
        when(templateService.getAvailableExercises(userId)).thenReturn(List.of());

        mockMvc.perform(get("/templates/{id}/edit", templateId))
                .andExpect(status().isOk());
    }

    @Test
    void editForm_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.findByIdAndOwner(templateId, userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/templates/{id}/edit", templateId))
                .andExpect(status().isNotFound());
    }

    @Test
    void templateExercisesEndpoint_returnsJson() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        WorkoutTemplate template = new WorkoutTemplate();
        template.setId(templateId);
        template.setOwnerUserId(userId);
        template.setName("Test");

        Exercise exercise = Exercise.builder()
                .id(exerciseId)
                .name("Bench Press")
                .primaryMuscle(MuscleGroup.CHEST)
                .build();

        TemplateItem item = new TemplateItem();
        item.setTemplateId(templateId);
        item.setExerciseId(exerciseId);
        item.setTargetSets(3);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.findByIdAndOwner(templateId, userId)).thenReturn(Optional.of(template));
        when(templateService.getTemplateItems(templateId)).thenReturn(List.of(item));
        when(templateService.getExercisesByIds(anyList())).thenReturn(Map.of(exerciseId, exercise));

        mockMvc.perform(get("/templates/{id}/exercises", templateId))
                .andExpect(status().isOk());
    }

    @Test
    void create_withValidationErrors_returnsCreatePage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.getAvailableExercises(any())).thenReturn(List.of());

        mockMvc.perform(post("/templates/create")
                        .param("name", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("create"));
    }

    @Test
    void create_success_redirectsToList() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        WorkoutTemplate createdTemplate = new WorkoutTemplate();
        createdTemplate.setId(UUID.randomUUID());
        createdTemplate.setName("Test Template");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.createTemplate(any(), anyString(), anyList())).thenReturn(createdTemplate);
        when(templateService.getAvailableExercises(any())).thenReturn(List.of());

        mockMvc.perform(post("/templates/create")
                        .param("name", "Test Template")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));
    }

    @Test
    void delete_success_redirectsToList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doNothing().when(templateService).deleteTemplate(any(), any());

        mockMvc.perform(post("/templates/{id}/delete", templateId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));
    }

    @Test
    void editSave_withValidationErrors_returnsEditPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(true);

        WorkoutTemplate template = new WorkoutTemplate();
        template.setId(templateId);
        template.setOwnerUserId(userId);
        template.setName("Test Template");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.findByIdAndOwner(templateId, userId)).thenReturn(Optional.of(template));
        when(templateService.getAvailableExercises(any())).thenReturn(List.of());

        mockMvc.perform(post("/templates/{id}/edit", templateId)
                        .param("name", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("edit"));
    }

    @Test
    void editSave_success_redirectsToList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        WorkoutTemplate template = new WorkoutTemplate();
        template.setId(templateId);
        template.setOwnerUserId(userId);
        template.setName("Test Template");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.findByIdAndOwner(templateId, userId)).thenReturn(Optional.of(template));
        doNothing().when(templateService).updateTemplate(any(), any(), anyString(), anyList());
        when(templateService.getAvailableExercises(any())).thenReturn(List.of());
        when(templateService.getTemplateItems(any())).thenReturn(List.of());

        mockMvc.perform(post("/templates/{id}/edit", templateId)
                        .param("name", "Updated Template")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));
    }
}

