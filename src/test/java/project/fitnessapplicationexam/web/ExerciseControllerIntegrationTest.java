package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.exercise.service.ExerciseService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExerciseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExerciseControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@SuppressWarnings("removal") @MockBean private ExerciseService exerciseService;
	@SuppressWarnings("removal") @MockBean private UserService userService;
	@SuppressWarnings("removal") @MockBean private ExerciseRepository exerciseRepository;

	@Test
	@WithMockUser
	void getExercises_returnsOk() throws Exception {
		User user = User.builder().id(UUID.randomUUID()).username("testuser").role(UserRole.USER).build();
		when(userService.findByUsernameOrThrow(any())).thenReturn(user);
		when(exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(any())).thenReturn(List.of());

		CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
		mockMvc.perform(get("/exercises").requestAttr(CsrfToken.class.getName(), token).requestAttr("_csrf", token))
				.andExpect(status().isOk());
	}
}

