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
import project.fitnessapplicationexam.template.form.TemplateForm;
import project.fitnessapplicationexam.template.form.TemplateItemForm;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import project.fitnessapplicationexam.config.ValidationConstants;

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
    public UUID create(UUID ownerUserId, TemplateForm form) {
        String name = (form.getName() == null) ? "" : form.getName().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Template name is required.");
        }
        if (workoutTemplateRepository.existsByOwnerUserIdAndNameIgnoreCase(ownerUserId, name)) {
            throw new IllegalArgumentException("You already have a template with that name.");
        }

        WorkoutTemplate template = workoutTemplateRepository.save(
                WorkoutTemplate.builder()
                        .ownerUserId(ownerUserId)
                        .name(name)
                        .build()
        );

        List<TemplateItemForm> rows = new ArrayList<>(
                Optional.ofNullable(form.getItems()).orElse(List.of())
        );
        rows.removeIf(row -> row == null || row.getExerciseId() == null);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Add at least one exercise.");
        }

        List<TemplateItem> items = new ArrayList<>(rows.size());
        for (TemplateItemForm row : rows) {
            exerciseRepository.findById(row.getExerciseId())
                    .filter(exercise -> exercise.getOwnerUserId().equals(ownerUserId)
                            || exercise.getOwnerUserId().equals(SystemDefault.SYSTEM_USER_ID))
                    .orElseThrow(() -> new IllegalArgumentException("Exercise not found or not allowed."));

            int sets = (row.getSets() == null) ? ValidationConstants.DEFAULT_TEMPLATE_SETS 
                    : Math.max(ValidationConstants.MIN_TEMPLATE_SETS, Math.min(ValidationConstants.MAX_TEMPLATE_SETS, row.getSets()));
            Integer order = (row.getOrderIndex() == null) ? Integer.MAX_VALUE : Math.max(0, row.getOrderIndex());

            items.add(TemplateItem.builder()
                    .templateId(template.getId())
                    .exerciseId(row.getExerciseId())
                    .targetSets(sets)
                    .position(order)
                    .build());
        }

        items.sort(Comparator.comparing(TemplateItem::getPosition));
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }

        templateItemRepository.saveAll(items);
        log.info("Template '{}' created for user {} with {} exercises", name, ownerUserId, items.size());
        return template.getId();
    }

    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public WorkoutTemplate createTemplate(UUID ownerId, String name, List<TemplateItemData> items) {
        WorkoutTemplate template = WorkoutTemplate.builder()
                .ownerUserId(ownerId)
                .name(name.trim())
                .createdOn(LocalDateTime.now())
                .build();
        workoutTemplateRepository.save(template);

        if (items != null && !items.isEmpty()) {
            List<TemplateItem> templateItems = buildTemplateItems(items, template.getId(), ownerId);
            templateItemRepository.saveAll(templateItems);
        }

        log.info("Template '{}' created for user {}", name, ownerId);
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
