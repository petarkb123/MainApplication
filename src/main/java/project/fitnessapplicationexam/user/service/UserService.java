package project.fitnessapplicationexam.user.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.repository.UserRepository;
import java.util.UUID;
import project.fitnessapplicationexam.config.ValidationConstants;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public User register(String username, String rawPwd, String email, String first, String last) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email in use");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .firstName(first)
                .lastName(last)
                .passwordHash(passwordEncoder.encode(rawPwd))
                .role(UserRole.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {} ({})", username, saved.getId());
        return saved;
    }

    @Transactional
    public void changeUsername(UUID userId, String newUsername) {
        String username = newUsername == null ? "" : newUsername.trim();
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (username.length() > ValidationConstants.MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username too long.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("That username is already taken.");
        }

        User user = userRepository.findById(userId).orElseThrow();
        String oldUsername = user.getUsername();
        user.setUsername(username);
        userRepository.save(user);
        log.info("Username changed for user {}: {} -> {}", userId, oldUsername, username);
    }
}
