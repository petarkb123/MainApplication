package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @SuppressWarnings("removal")
    @MockBean
    private WorkoutSessionRepository sessionRepository;

    @Test
    void index_unauthenticated_returnsIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void index_authenticated_showsUserInfo() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .profilePicture("avatar.jpg")
                .role(UserRole.USER)
                .build();

        when(userService.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findTop5ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

        mockMvc.perform(get("/")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("navAvatar"))
                .andExpect(model().attributeExists("username"))
                .andExpect(model().attributeExists("recentWorkouts"));
    }

    @Test
    void index_withRecentWorkouts_showsWorkouts() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .profilePicture("avatar.jpg")
                .role(UserRole.USER)
                .build();

        WorkoutSession session = new WorkoutSession();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);

        when(userService.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findTop5ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of(session));

        mockMvc.perform(get("/")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("recentWorkouts"));
    }

    @Test
    void home_returnsIndexPage() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void terms_returnsTermsPage() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("terms"));
    }

    @Test
    void privacy_returnsPrivacyPage() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("privacy"));
    }
}

