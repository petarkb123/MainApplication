package project.fitnessapplicationexam.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.repository.UserRepository;
import project.fitnessapplicationexam.user.service.UserSettingsService;
import project.fitnessapplicationexam.common.exceptions.InvalidAvatarUrlException;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .build();
    }

    @Test
    void requireByUsername_found_returnsUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userSettingsService.requireByUsername("testuser");
        assertEquals(user, result);
    }

    @Test
    void requireByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userSettingsService.requireByUsername("unknown"));
    }

    @Test
    void setAvatarUrl_validUrl_setsAvatar() {
        String validUrl = "https://example.com/avatar.jpg";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userSettingsService.setAvatarUrl(userId, validUrl);

        verify(userRepository).save(user);
        assertEquals(validUrl, user.getProfilePicture());
    }

    @Test
    void setAvatarUrl_emptyUrl_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(InvalidAvatarUrlException.class, () -> userSettingsService.setAvatarUrl(userId, ""));
    }

    @Test
    void setAvatarUrl_invalidUrl_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(InvalidAvatarUrlException.class, () -> userSettingsService.setAvatarUrl(userId, "invalid-url"));
    }

    @Test
    void removeAvatar_setsAvatarToNull() {
        user.setProfilePicture("https://example.com/avatar.jpg");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userSettingsService.removeAvatar(userId);

        verify(userRepository).save(user);
        assertNull(user.getProfilePicture());
    }

    @Test
    void changePassword_success() {
        String currentRaw = "currentPassword";
        String newRaw = "newPassword123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentRaw, user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(newRaw)).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userSettingsService.changePassword(userId, currentRaw, newRaw, newRaw);

        verify(passwordEncoder).encode(newRaw);
        verify(userRepository).save(user);
        assertEquals("newHashedPassword", user.getPasswordHash());
    }

    @Test
    void changePassword_passwordsDoNotMatch_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                userSettingsService.changePassword(userId, "current", "new1", "new2"));
    }

    @Test
    void changePassword_passwordTooShort_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                userSettingsService.changePassword(userId, "current", "short", "short"));
    }

    @Test
    void changePassword_currentPasswordIncorrect_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
                userSettingsService.changePassword(userId, "wrong", "newPassword123", "newPassword123"));
    }
}
