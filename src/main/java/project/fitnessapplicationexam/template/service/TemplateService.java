package project.fitnessapplicationexam.template.service;

import lombok.RequiredArgsConstructor;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final WorkoutTemplateRepository templateRepo;
    private final TemplateItemRepository itemRepo;
    private final ExerciseRepository exerciseRepo;

    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#owner")
    public List<WorkoutTemplate> list(UUID owner) {
        return templateRepo.findAllByOwnerUserIdOrderByCreatedOnDesc(owner);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "exercises", key = "#userId")
    public List<Exercise> getAvailableExercises(UUID userId) {
        List<UUID> owners = List.of(SystemDefault.SYSTEM_USER_ID, userId);
        return exerciseRepo.findAllByOwnerUserIdInOrderByNameAsc(owners);
    }
    
    @Transactional(readOnly = true)
    public Optional<WorkoutTemplate> findByIdAndOwner(UUID templateId, UUID ownerId) {
        return templateRepo.findByIdAndOwnerUserId(templateId, ownerId);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateItem> getTemplateItems(UUID templateId) {
        return itemRepo.findAllByTemplateIdOrderByPositionAsc(templateId);
    }
    
    @Transactional(readOnly = true)
    public Map<UUID, Exercise> getExercisesByIds(List<UUID> exerciseIds) {
        return exerciseRepo.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));
    }

    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public UUID create(UUID ownerUserId, TemplateForm form) {
        String name = (form.getName() == null) ? "" : form.getName().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Template name is required.");
        }
        if (templateRepo.existsByOwnerUserIdAndNameIgnoreCase(ownerUserId, name)) {
            throw new IllegalArgumentException("You already have a template with that name.");
        }

        WorkoutTemplate tpl = templateRepo.save(
                WorkoutTemplate.builder()
                        .ownerUserId(ownerUserId)
                        .name(name)
                        .build()
        );

        List<TemplateItemForm> rows = new ArrayList<>(
                Optional.ofNullable(form.getItems()).orElse(List.of())
        );
        rows.removeIf(r -> r == null || r.getExerciseId() == null);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Add at least one exercise.");
        }

        List<TemplateItem> items = new ArrayList<>(rows.size());
        for (TemplateItemForm r : rows) {
             exerciseRepo.findById(r.getExerciseId())
                    .filter(ex -> ex.getOwnerUserId().equals(ownerUserId)
                            || ex.getOwnerUserId().equals(SystemDefault.SYSTEM_USER_ID))
                    .orElseThrow(() -> new IllegalArgumentException("Exercise not found or not allowed."));

            int sets = (r.getSets() == null) ? 3 : Math.max(1, Math.min(20, r.getSets()));
            Integer order = (r.getOrderIndex() == null) ? Integer.MAX_VALUE : Math.max(0, r.getOrderIndex());

            items.add(TemplateItem.builder()
                    .templateId(tpl.getId())
                    .exerciseId(r.getExerciseId())
                    .targetSets(sets)
                    .position(order)
                    .build());
        }

        items.sort(Comparator.comparing(TemplateItem::getPosition));
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }

        itemRepo.saveAll(items);
        return tpl.getId();
    }
    
    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public WorkoutTemplate createTemplate(UUID ownerId, String name, List<TemplateItemData> items) {
        WorkoutTemplate tpl = WorkoutTemplate.builder()
                .ownerUserId(ownerId)
                .name(name.trim())
                .createdOn(LocalDateTime.now())
                .build();
        templateRepo.save(tpl);
        
        if (items != null && !items.isEmpty()) {
            List<UUID> exerciseIds = items.stream()
                    .map(TemplateItemData::exerciseId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            
            Map<UUID, Exercise> exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
                    .collect(Collectors.toMap(Exercise::getId, e -> e));
            
            ArrayList<TemplateItem> toSave = new ArrayList<>();
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
                
                toSave.add(TemplateItem.builder()
                        .templateId(tpl.getId())
                        .exercise(exercise)
                        .targetSets(item.targetSets())
                        .position(item.position())
                        .groupId(item.groupId())
                        .groupType(item.groupType())
                        .groupOrder(item.groupOrder())
                        .setNumber(item.setNumber())
                        .build());
            }
            itemRepo.saveAll(toSave);
        }
        
        return tpl;
    }
    
    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public void deleteTemplate(UUID templateId, UUID ownerId) {
        templateRepo.findByIdAndOwnerUserId(templateId, ownerId).ifPresent(tpl -> {
            itemRepo.deleteByTemplateId(templateId);
            templateRepo.delete(tpl);
        });
    }
    
    @Transactional
    @CacheEvict(value = {"templates", "exercises"}, allEntries = true)
    public void updateTemplate(UUID templateId, UUID ownerId, String newName, List<TemplateItemData> items) {
        WorkoutTemplate tpl = templateRepo.findByIdAndOwnerUserId(templateId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        tpl.setName(newName.trim());
        templateRepo.save(tpl);
        
        itemRepo.deleteByTemplateId(templateId);
        
        if (items != null && !items.isEmpty()) {
            List<UUID> exerciseIds = items.stream()
                    .map(TemplateItemData::exerciseId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            
            Map<UUID, Exercise> exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
                    .collect(Collectors.toMap(Exercise::getId, e -> e));
            
            ArrayList<TemplateItem> toSave = new ArrayList<>();
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
                
                toSave.add(TemplateItem.builder()
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
            itemRepo.saveAll(toSave);
        }
    }
    
    public boolean isNameTaken(UUID ownerId, String name) {
        return templateRepo.existsByOwnerUserIdAndNameIgnoreCase(ownerId, name);
    }
}
