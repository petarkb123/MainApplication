package project.fitnessapplication.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplication.exercise.model.Exercise;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findAllByOwnerUserId(UUID ownerId);

    List<Exercise> findAllByOwnerUserIdOrderByCreatedOnDesc(UUID ownerUserId);

    Optional<Exercise> findByIdAndOwnerUserId(UUID id, UUID ownerId);

    List<Exercise> findAllByOwnerUserIdInOrderByNameAsc(Collection<UUID> ownerIds);

    Optional<Exercise> findByIdAndOwnerUserIdIn(UUID id, Collection<UUID> ownerIds);

    Optional<Exercise> findByNameIgnoreCaseAndOwnerUserId(String name, UUID ownerUserId);



}
