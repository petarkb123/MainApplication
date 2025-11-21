package project.fitnessapplication.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplication.schedule.dto.CreateScheduledWorkoutRequest;
import project.fitnessapplication.schedule.dto.ScheduledWorkoutDto;
import project.fitnessapplication.schedule.model.ScheduledWorkout;
import project.fitnessapplication.schedule.repository.ScheduledWorkoutRepository;
import project.fitnessapplication.template.model.WorkoutTemplate;
import project.fitnessapplication.template.repository.WorkoutTemplateRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduledWorkoutRepository repo;
    private final WorkoutTemplateRepository templates;

    public ScheduledWorkoutDto create(UUID userId, CreateScheduledWorkoutRequest req) {
        WorkoutTemplate tpl = templates.findByIdAndOwnerUserId(req.templateId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        ScheduledWorkout sw = repo.save(ScheduledWorkout.builder()
                .userId(userId)
                .templateId(tpl.getId())
                .date(req.date())
                .notes(req.notes())
                .build());
        return ScheduledWorkoutDto.fromEntity(sw, tpl.getName());
    }

    @Transactional(readOnly = true)
    public List<ScheduledWorkoutDto> findInRange(UUID userId, LocalDate from, LocalDate to) {
        var list = repo.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
        var tplNames = templates.findAllById(list.stream().map(ScheduledWorkout::getTemplateId).toList())
                .stream().collect(java.util.stream.Collectors.toMap(WorkoutTemplate::getId, WorkoutTemplate::getName));
        return list.stream()
                .map(sw -> ScheduledWorkoutDto.fromEntity(sw, tplNames.getOrDefault(sw.getTemplateId(), "Template")))
                .toList();
    }

    public void delete(UUID userId, UUID id) {
        repo.findById(id).filter(sw -> sw.getUserId().equals(userId)).ifPresent(repo::delete);
    }
}
