package project.fitnessapplication.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplication.user.model.User;
import project.fitnessapplication.user.model.UserRole;
import project.fitnessapplication.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public User getOrThrow(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User findByIdOrThrow(UUID id) { return getOrThrow(id); }

    @Transactional(readOnly = true)
    public User findByUsernameOrThrow(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }


    @Transactional
    public User register(String username, String rawPwd, String email, String first, String last) {
        if (repo.existsByUsername(username)) throw new IllegalArgumentException("Username taken");
        if (repo.existsByEmail(email)) throw new IllegalArgumentException("Email in use");

        User u = User.builder()
                .username(username)
                .email(email)
                .firstName(first)
                .lastName(last)
                .passwordHash(encoder.encode(rawPwd))
                .role(UserRole.USER)
                .build();

        return repo.save(u);
    }

    @Transactional
    public void changeUsername(UUID userId, String newUsername) {
        String uname = newUsername == null ? "" : newUsername.trim();
        if (uname.isBlank()) throw new IllegalArgumentException("Username is required.");
        if (uname.length() > 64) throw new IllegalArgumentException("Username too long.");
        if (repo.existsByUsernameIgnoreCase(uname))
            throw new IllegalArgumentException("That username is already taken.");

        var user = repo.findById(userId).orElseThrow();
        user.setUsername(uname);
        repo.save(user);
    }



}
