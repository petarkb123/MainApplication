package project.fitnessapplicationexam.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.fitnessapplicationexam.template.model.TemplateItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateItemRepository extends JpaRepository<TemplateItem, UUID> {
    List<TemplateItem> findAllByTemplateIdOrderByPositionAsc(UUID templateId);

    void deleteByTemplateId(UUID templateId);

    void deleteByExerciseId(UUID exerciseId);
}
