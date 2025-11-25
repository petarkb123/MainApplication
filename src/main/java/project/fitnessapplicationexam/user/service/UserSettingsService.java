package project.fitnessapplicationexam.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.common.exceptions.InvalidAvatarUrlException;
import project.fitnessapplicationexam.user.repository.UserRepository;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import project.fitnessapplicationexam.config.ValidationConstants;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    @Transactional
    public void setAvatarUrl(UUID userId, String url) {
        User user = userRepository.findById(userId).orElseThrow();
        String trimmedUrl = (url == null) ? "" : url.trim();
        if (trimmedUrl.isEmpty()) {
            throw new InvalidAvatarUrlException("Avatar URL is required.");
        }
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            throw new InvalidAvatarUrlException("Avatar URL must start with http:// or https://");
        }
        user.setProfilePicture(trimmedUrl);
        userRepository.save(user);
    }

    @Transactional
    public void removeAvatar(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setProfilePicture(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentRaw, String newRaw, String confirmRaw) {
        if (!Objects.equals(newRaw, confirmRaw)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (newRaw == null || newRaw.length() < ValidationConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("New password must be at least " + ValidationConstants.MIN_PASSWORD_LENGTH + " characters.");
        }
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(currentRaw, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newRaw));
        userRepository.save(user);
    }
}
