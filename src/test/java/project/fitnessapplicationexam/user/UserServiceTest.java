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
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	@Test
	void register_createsUser() {
		when(userRepository.existsByUsername("john")).thenReturn(false);
		when(userRepository.existsByEmail("j@e.com")).thenReturn(false);
		when(passwordEncoder.encode("pwd")).thenReturn("enc");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User user = userService.register("john", "pwd", "j@e.com", "John", "D");
		assertEquals("john", user.getUsername());
		assertEquals(UserRole.USER, user.getRole());
		assertEquals("enc", user.getPasswordHash());
	}

	@Test
	void register_usernameTaken_throwsException() {
		when(userRepository.existsByUsername("john")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> userService.register("john", "pwd", "j@e.com", "John", "D"));
	}

	@Test
	void register_emailInUse_throwsException() {
		when(userRepository.existsByUsername("john")).thenReturn(false);
		when(userRepository.existsByEmail("j@e.com")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> userService.register("john", "pwd", "j@e.com", "John", "D"));
	}

	@Test
	void changeUsername_updatesUsername() {
		UUID id = UUID.randomUUID();
		User user = User.builder().id(id).username("old").build();
		when(userRepository.existsByUsernameIgnoreCase("new")).thenReturn(false);
		when(userRepository.findById(id)).thenReturn(Optional.of(user));

		userService.changeUsername(id, "new");
		assertEquals("new", user.getUsername());
		verify(userRepository).save(user);
	}

	@Test
	void changeUsername_blank_throwsException() {
		UUID id = UUID.randomUUID();
		assertThrows(IllegalArgumentException.class, () -> userService.changeUsername(id, " "));
	}

	@Test
	void changeUsername_null_throwsException() {
		UUID id = UUID.randomUUID();
		assertThrows(IllegalArgumentException.class, () -> userService.changeUsername(id, null));
	}

	@Test
	void changeUsername_tooLong_throwsException() {
		UUID id = UUID.randomUUID();
		String tooLong = "a".repeat(65);
		assertThrows(IllegalArgumentException.class, () -> userService.changeUsername(id, tooLong));
	}

	@Test
	void changeUsername_duplicate_throwsException() {
		UUID id = UUID.randomUUID();
		when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> userService.changeUsername(id, "taken"));
	}

	@Test
	void findByIdOrThrow_found_returnsUser() {
		UUID id = UUID.randomUUID();
		User user = User.builder().id(id).build();
		when(userRepository.findById(id)).thenReturn(Optional.of(user));

		User result = userService.findByIdOrThrow(id);
		assertEquals(user, result);
	}

	@Test
	void findByIdOrThrow_notFound_throwsException() {
		UUID id = UUID.randomUUID();
		when(userRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> userService.findByIdOrThrow(id));
	}

	@Test
	void findByUsernameOrThrow_found_returnsUser() {
		User user = User.builder().username("john").build();
		when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

		User result = userService.findByUsernameOrThrow("john");
		assertEquals(user, result);
	}

	@Test
	void findByUsernameOrThrow_notFound_throwsException() {
		when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> userService.findByUsernameOrThrow("john"));
	}
}
