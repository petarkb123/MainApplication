package project.fitnessapplicationexam.template.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplicationexam.config.SystemDefault;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.template.dto.TemplateItemData;
import project.fitnessapplicationexam.template.model.TemplateItem;
import project.fitnessapplicationexam.template.model.WorkoutTemplate;
import project.fitnessapplicationexam.template.repository.TemplateItemRepository;
import project.fitnessapplicationexam.template.repository.WorkoutTemplateRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateItemRepository templateItemRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#owner")
    public List<WorkoutTemplate> list(UUID owner) {
        return workoutTemplateRepository.findAllByOwnerUserIdOrderByCreatedOnDesc(owner);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "exercises", key = "#userId")
    public List<Exercise> getAvailableExercises(UUID userId) {
        List<UUID> owners = List.of(SystemDefault.SYSTEM_USER_ID, userId);
        return exerciseRepository.findAllByOwnerUserIdInOrderByNameAsc(owners);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutTemplate> findByIdAndOwner(UUID templateId, UUID ownerId) {
        return workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId);
    }

    @Transactional(readOnly = true)
    public List<TemplateItem> getTemplateItems(UUID templateId) {
        return templateItemRepository.findAllByTemplateIdOrderByPositionAsc(templateId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Exercise> getExercisesByIds(List<UUID> exerciseIds) {
        return exerciseRepository.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, exercise -> exercise));
    }

    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public WorkoutTemplate createTemplate(UUID ownerId, String name, List<TemplateItemData> items) {
        String trimmedName = (name == null) ? "" : name.trim();
        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException("Template name is required.");
        }
        
        if (workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(ownerId, trimmedName)) {
            throw new IllegalArgumentException("You already have a template with that name.");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Add at least one exercise.");
        }

        WorkoutTemplate template = WorkoutTemplate.builder()
                .ownerUserId(ownerId)
                .name(trimmedName)
                .createdOn(LocalDateTime.now())
                .build();
        workoutTemplateRepository.save(template);

        List<TemplateItem> templateItems = buildTemplateItems(items, template.getId(), ownerId);
        templateItemRepository.saveAll(templateItems);

        log.info("Template '{}' created for user {} with {} exercises", trimmedName, ownerId, templateItems.size());
        return template;
    }

    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public void deleteTemplate(UUID templateId, UUID ownerId) {
        workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId).ifPresent(template -> {
            templateItemRepository.deleteByTemplateId(templateId);
            workoutTemplateRepository.delete(template);
            log.info("Template '{}' deleted for user {}", template.getName(), ownerId);
        });
    }

    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public void updateTemplate(UUID templateId, UUID ownerId, String newName, List<TemplateItemData> items) {
        WorkoutTemplate template = workoutTemplateRepository.findByIdAndOwnerUserId(templateId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String oldName = template.getName();
        template.setName(newName.trim());
        workoutTemplateRepository.save(template);

        templateItemRepository.deleteByTemplateId(templateId);

        if (items != null && !items.isEmpty()) {
            List<TemplateItem> templateItems = buildTemplateItems(items, templateId, ownerId);
            templateItemRepository.saveAll(templateItems);
        }

        log.info("Template updated for user {}: '{}' -> '{}'", ownerId, oldName, newName);
    }

    public boolean isNameTaken(UUID ownerId, String name) {
        return workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(ownerId, name);
    }

    private List<TemplateItem> buildTemplateItems(List<TemplateItemData> items, UUID templateId, UUID ownerId) {
        List<UUID> exerciseIds = items.stream()
                .map(TemplateItemData::exerciseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, Exercise> exerciseMap = exerciseRepository.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, exercise -> exercise));
        
        List<TemplateItem> templateItems = new ArrayList<>();
        for (TemplateItemData item : items) {
            if (item.exerciseId() == null) {
                continue;
            }
            
            Exercise exercise = exerciseMap.get(item.exerciseId());
            if (exercise == null) {
                throw new IllegalArgumentException("Exercise not found: " + item.exerciseId());
            }
            
            if (!exercise.getOwnerUserId().equals(ownerId)
                    && !exercise.getOwnerUserId().equals(SystemDefault.SYSTEM_USER_ID)) {
                throw new IllegalArgumentException("Exercise not accessible: " + item.exerciseId());
            }
            
            templateItems.add(TemplateItem.builder()
                    .templateId(templateId)
                    .exercise(exercise)
                    .targetSets(item.targetSets())
                    .position(item.position())
                    .groupId(item.groupId())
                    .groupType(item.groupType())
                    .groupOrder(item.groupOrder())
                    .setNumber(item.setNumber())
                    .build());
        }
        return templateItems;
    }
}
