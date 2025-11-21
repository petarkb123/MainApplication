package project.fitnessapplication.template.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.fitnessapplication.config.SystemDefault;
import project.fitnessapplication.exercise.model.Exercise;
import project.fitnessapplication.exercise.repository.ExerciseRepository;
import project.fitnessapplication.template.dto.TemplateItemData;
import project.fitnessapplication.template.model.TemplateItem;
import project.fitnessapplication.template.model.WorkoutTemplate;
import project.fitnessapplication.template.repository.TemplateItemRepository;
import project.fitnessapplication.template.repository.WorkoutTemplateRepository;
import project.fitnessapplication.template.form.TemplateForm;
import project.fitnessapplication.template.form.TemplateItemForm;

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
    public List<WorkoutTemplate> list(UUID owner) {
        return templateRepo.findAllByOwnerUserIdOrderByCreatedOnDesc(owner);
    }
    
    @Transactional(readOnly = true)
    public List<Exercise> getAvailableExercises(UUID userId) {
        var owners = List.of(SystemDefault.SYSTEM_USER_ID, userId);
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
    public WorkoutTemplate createTemplate(UUID ownerId, String name, List<TemplateItemData> items) {
        var tpl = WorkoutTemplate.builder()
                .ownerUserId(ownerId)
                .name(name.trim())
                .createdOn(LocalDateTime.now())
                .build();
        templateRepo.save(tpl);
        
        if (items != null && !items.isEmpty()) {
            var toSave = new ArrayList<TemplateItem>();
            for (var item : items) {
                toSave.add(TemplateItem.builder()
                        .templateId(tpl.getId())
                        .exerciseId(item.exerciseId())
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
    public void deleteTemplate(UUID templateId, UUID ownerId) {
        templateRepo.findByIdAndOwnerUserId(templateId, ownerId).ifPresent(tpl -> {
            itemRepo.deleteByTemplateId(templateId);
            templateRepo.delete(tpl);
        });
    }
    
    @Transactional
    public void updateTemplate(UUID templateId, UUID ownerId, String newName, List<TemplateItemData> items) {
        var tpl = templateRepo.findByIdAndOwnerUserId(templateId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        tpl.setName(newName.trim());
        templateRepo.save(tpl);
        
        itemRepo.deleteByTemplateId(templateId);
        
        if (items != null && !items.isEmpty()) {
            var toSave = new ArrayList<TemplateItem>();
            for (var item : items) {
                toSave.add(TemplateItem.builder()
                        .templateId(templateId)
                        .exerciseId(item.exerciseId())
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
