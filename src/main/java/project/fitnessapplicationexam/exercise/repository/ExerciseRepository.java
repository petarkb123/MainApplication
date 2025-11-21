package project.fitnessapplicationexam.exercise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplicationexam.exercise.model.Exercise;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findAllByOwnerUserId(UUID ownerId);

    Optional<Exercise> findByIdAndOwnerUserId(UUID id, UUID ownerId);

    List<Exercise> findAllByOwnerUserIdInOrderByNameAsc(Collection<UUID> ownerIds);

    Optional<Exercise> findByNameIgnoreCaseAndOwnerUserId(String name, UUID ownerUserId);



}
