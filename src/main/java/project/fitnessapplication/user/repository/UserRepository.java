package project.fitnessapplication.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.fitnessapplication.user.model.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Query("update User u set u.profilePicture = :path where u.id = :id")
    int updateAvatar(@Param("id") UUID id, @Param("path") String path);

    @Modifying
    @Query("update User u set u.passwordHash = :hash where u.id = :id")
    int updatePassword(@Param("id") UUID id, @Param("hash") String hash);

}
