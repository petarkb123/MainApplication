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
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSubscriptionService;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(SubscriptionController.class)
@WithMockUser
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @SuppressWarnings("removal")
    @MockBean
    private UserSubscriptionService userSubscriptionService;

    @Test
    void selectTier_basic_activatesBasic() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doNothing().when(userSubscriptionService).activateBasic(any(UUID.class));

        mockMvc.perform(post("/subscription/select")
                        .param("tier", "BASIC")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"));

        verify(userSubscriptionService).activateBasic(userId);
    }

    @Test
    void selectTier_pro_activatesPro() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        doNothing().when(userSubscriptionService).activatePro(any(UUID.class));

        mockMvc.perform(post("/subscription/select")
                        .param("tier", "PRO")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"));

        verify(userSubscriptionService).activatePro(userId);
    }

    @Test
    void subscriptionPage_returnsSubscriptionPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);

        mockMvc.perform(get("/subscription"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription"));
    }

    @Test
    void subscriptionPage_showsProBadge() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setProfilePicture("avatar.jpg");
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);

        mockMvc.perform(get("/subscription"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription"));
    }
}

