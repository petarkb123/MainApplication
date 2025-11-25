package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.repository.UserRepository;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSubscriptionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@WithMockUser(roles = "ADMIN")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private UserRepository userRepository;

    @SuppressWarnings("removal")
    @MockBean
    private UserSubscriptionService subscriptionService;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @Test
    void users_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        user.setProfilePicture("avatar.jpg");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(userRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"));
    }

    @Test
    void deactivateAccount_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setUsername("admin");
        currentUser.setRole(UserRole.ADMIN);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setUsername("target");
        targetUser.setActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(currentUser);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        mockMvc.perform(post("/admin/users/{id}/deactivate-account", targetUserId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userRepository).save(argThat(u -> !u.isActive()));
    }

    @Test
    void deactivateAccount_selfPrevented_redirectsWithError() throws Exception {
        UUID userId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setUsername("admin");
        currentUser.setRole(UserRole.ADMIN);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(currentUser);

        mockMvc.perform(post("/admin/users/{id}/deactivate-account", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void activateAccount_success() throws Exception {
        UUID userId = UUID.randomUUID();
        User targetUser = new User();
        targetUser.setId(userId);
        targetUser.setUsername("target");
        targetUser.setActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        mockMvc.perform(post("/admin/users/{id}/activate-account", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userRepository).save(argThat(u -> u.isActive()));
    }

    @Test
    void toggleSubscription_proToBasic_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setSubscriptionTier(SubscriptionTier.PRO);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(currentUser);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(post("/admin/users/{id}/toggle-subscription", targetUserId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(subscriptionService).activateBasic(targetUserId);
    }

    @Test
    void toggleSubscription_basicToPro_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setSubscriptionTier(SubscriptionTier.BASIC);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(currentUser);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        mockMvc.perform(post("/admin/users/{id}/toggle-subscription", targetUserId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(subscriptionService).activatePro(targetUserId);
    }

    @Test
    void makeAdmin_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setRole(UserRole.USER);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(currentUser);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        mockMvc.perform(post("/admin/users/{id}/make-admin", targetUserId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userRepository).save(argThat(u -> u.getRole() == UserRole.ADMIN));
    }

    @Test
    void removeAdmin_success() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setRole(UserRole.ADMIN);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(currentUser);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        mockMvc.perform(post("/admin/users/{id}/remove-admin", targetUserId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userRepository).save(argThat(u -> u.getRole() == UserRole.USER));
    }
}


