package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.template.model.TemplateItem;
import project.fitnessapplicationexam.template.model.WorkoutTemplate;
import project.fitnessapplicationexam.template.service.TemplateService;
import project.fitnessapplicationexam.workout.model.SessionStatus;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.model.WorkoutSet;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.workout.service.WorkoutService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkoutController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class WorkoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @SuppressWarnings("removal")
    @MockBean
    private WorkoutService workoutService;

    @SuppressWarnings("removal")
    @MockBean
    private TemplateService templateService;

    @SuppressWarnings("removal")
    @MockBean
    private WorkoutSessionRepository sessionRepository;

    @Test
    void history_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.getRecentSessions(any(), anyInt())).thenReturn(List.of());
        when(templateService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/workouts"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"));
    }

    @Test
    void session_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);

        WorkoutSession session = new WorkoutSession();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.start(any())).thenReturn(session);
        when(workoutService.getAvailableExercises(any())).thenReturn(List.of());
        when(workoutService.getSessionSets(any())).thenReturn(List.of());

        mockMvc.perform(get("/workouts/session"))
                .andExpect(status().isOk())
                .andExpect(view().name("session"));
    }

    @Test
    void details_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);

        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setStatus(SessionStatus.FINISHED);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(get("/workouts/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(view().name("details"));
    }


    @Test
    void finishQuick_redirectsToList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        
        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doNothing().when(workoutService).finishSessionWithSets(any(), any(), any());

        mockMvc.perform(post("/workouts/{id}/finish", sessionId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workouts"));
    }

    @Test
    void details_notFound_redirectsWithError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.findById(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/workouts/{id}", sessionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workouts"));
    }

    @Test
    void details_wrongUser_redirectsWithError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");

        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(otherUserId);
        session.setStatus(SessionStatus.FINISHED);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(get("/workouts/{id}", sessionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workouts"));
    }

    @Test
    void deleteWorkout_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doNothing().when(workoutService).deleteSession(any(), any());

        mockMvc.perform(post("/workouts/{id}/delete", sessionId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workouts"));
    }

    @Test
    void session_withSessionId_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);

        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(userId);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.findById(sessionId)).thenReturn(Optional.of(session));
        when(workoutService.getAvailableExercises(any())).thenReturn(List.of());
        when(workoutService.getSessionSets(any())).thenReturn(List.of());

        mockMvc.perform(get("/workouts/session").param("sessionId", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("session"));
    }

    @Test
    void session_wrongUser_throwsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");

        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(otherUserId);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(get("/workouts/session").param("sessionId", sessionId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWorkout_notFound_returnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(workoutService).deleteSession(any(), any());

        mockMvc.perform(post("/workouts/{id}/delete", sessionId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void finishRich_missingSessionId_returnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);

        mockMvc.perform(post("/workouts/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finishRich_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doNothing().when(workoutService).finishSessionWithSets(any(), any(), any());

        String json = "{"
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"exercises\":[]"
                + "}";

        mockMvc.perform(post("/workouts/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void templateExercises_returnsList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        WorkoutTemplate template = new WorkoutTemplate();
        template.setId(templateId);
        template.setOwnerUserId(userId);

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

        mockMvc.perform(get("/workouts/templates/{templateId}/exercises", templateId))
                .andExpect(status().isOk());
    }

    @Test
    void templateExercises_templateNotFound_returnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(templateService.findByIdAndOwner(templateId, userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/workouts/templates/{templateId}/exercises", templateId))
                .andExpect(status().isNotFound());
    }

    @Test
    void details_withSets_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setProfilePicture("avatar.jpg");

        WorkoutSession session = new WorkoutSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setStatus(SessionStatus.FINISHED);

        WorkoutSet set = WorkoutSet.builder()
                .sessionId(sessionId)
                .exerciseId(exerciseId)
                .reps(10)
                .weight(new BigDecimal("100.0"))
                .setNumber(1)
                .build();

        Exercise exercise = Exercise.builder()
                .id(exerciseId)
                .name("Bench Press")
                .primaryMuscle(MuscleGroup.CHEST)
                .build();

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(workoutService.findById(sessionId)).thenReturn(Optional.of(session));
        when(workoutService.getSessionSets(sessionId)).thenReturn(List.of(set));
        when(workoutService.getExercisesByIds(anyList())).thenReturn(Map.of(exerciseId, exercise));

        mockMvc.perform(get("/workouts/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(view().name("details"));
    }
}

