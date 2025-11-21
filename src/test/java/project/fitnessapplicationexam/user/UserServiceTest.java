package project.fitnessapplicationexam.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.repository.UserRepository;
import project.fitnessapplicationexam.user.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository repo;

	@Mock
	private PasswordEncoder encoder;

	@InjectMocks
	private UserService service;

	@Test
	void register_success() {
		when(repo.existsByUsername("john")).thenReturn(false);
		when(repo.existsByEmail("j@e.com")).thenReturn(false);
		when(encoder.encode("pwd")).thenReturn("enc");
		when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User u = service.register("john", "pwd", "j@e.com", "John", "D");
		assertEquals("john", u.getUsername());
		assertEquals(UserRole.USER, u.getRole());
		assertEquals("enc", u.getPasswordHash());
	}

	@Test
	void register_usernameTaken_throwsException() {
		when(repo.existsByUsername("john")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> service.register("john", "pwd", "j@e.com", "John", "D"));
	}

	@Test
	void register_emailInUse_throwsException() {
		when(repo.existsByUsername("john")).thenReturn(false);
		when(repo.existsByEmail("j@e.com")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> service.register("john", "pwd", "j@e.com", "John", "D"));
	}

	@Test
	void changeUsername_happyPath() {
		UUID id = UUID.randomUUID();
		User u = User.builder().id(id).username("old").build();
		when(repo.existsByUsernameIgnoreCase("new")).thenReturn(false);
		when(repo.findById(id)).thenReturn(Optional.of(u));

		service.changeUsername(id, "new");
		assertEquals("new", u.getUsername());
		verify(repo).save(u);
	}

	@Test
	void changeUsername_rejectsBlank() {
		UUID id = UUID.randomUUID();
		assertThrows(IllegalArgumentException.class, () -> service.changeUsername(id, " "));
	}

	@Test
	void changeUsername_rejectsNull() {
		UUID id = UUID.randomUUID();
		assertThrows(IllegalArgumentException.class, () -> service.changeUsername(id, null));
	}

	@Test
	void changeUsername_rejectsTooLong() {
		UUID id = UUID.randomUUID();
		String tooLong = "a".repeat(65);
		assertThrows(IllegalArgumentException.class, () -> service.changeUsername(id, tooLong));
	}

	@Test
	void changeUsername_rejectsDuplicate() {
		UUID id = UUID.randomUUID();
		when(repo.existsByUsernameIgnoreCase("taken")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> service.changeUsername(id, "taken"));
	}

	@Test
	void getOrThrow_found_returnsUser() {
		UUID id = UUID.randomUUID();
		User user = User.builder().id(id).build();
		when(repo.findById(id)).thenReturn(Optional.of(user));

		User result = service.getOrThrow(id);
		assertEquals(user, result);
	}

	@Test
	void getOrThrow_notFound_throwsException() {
		UUID id = UUID.randomUUID();
		when(repo.findById(id)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.getOrThrow(id));
	}

	@Test
	void findByUsernameOrThrow_found_returnsUser() {
		User user = User.builder().username("john").build();
		when(repo.findByUsername("john")).thenReturn(Optional.of(user));

		User result = service.findByUsernameOrThrow("john");
		assertEquals(user, result);
	}

	@Test
	void findByUsernameOrThrow_notFound_throwsException() {
		when(repo.findByUsername("john")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.findByUsernameOrThrow("john"));
	}
}
