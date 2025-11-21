package project.fitnessapplication.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.fitnessapplication.template.model.TemplateItem;

import java.util.List;
import java.util.UUID;

public interface TemplateItemRepository extends JpaRepository<TemplateItem, UUID> {
    List<TemplateItem> findAllByTemplateIdOrderByPositionAsc(UUID templateId);
    long deleteByTemplateId(UUID templateId);

}
