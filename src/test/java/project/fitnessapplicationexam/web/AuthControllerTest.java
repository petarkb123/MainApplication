package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @Test
    void loginPage_returnsLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginPage_withError_showsErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void registerForm_returnsRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_success_redirectsToLogin() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .role(UserRole.USER)
                .build();

        when(userService.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(user);

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "password123")
                        .param("email", "test@example.com")
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("msg"));

        verify(userService).register("testuser", "password123", "test@example.com", "Test", "User");
    }

    @Test
    void register_validationError_returnsRegisterPage() throws Exception {
        when(userService.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existinguser")
                        .param("password", "password123")
                        .param("email", "test@example.com")
                        .param("firstName", "Test")
                        .param("lastName", "User")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        verify(userService).register(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}

