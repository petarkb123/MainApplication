package project.fitnessapplication.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplication.template.model.WorkoutTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, UUID> {
    List<WorkoutTemplate> findAllByOwnerUserIdOrderByCreatedOnDesc(UUID ownerId);

    boolean existsByOwnerUserIdAndNameIgnoreCase(UUID ownerUserId, String name);

    Optional<WorkoutTemplate> findByIdAndOwnerUserId(UUID id, UUID ownerId);

}
