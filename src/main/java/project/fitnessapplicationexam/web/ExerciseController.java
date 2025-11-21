package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplicationexam.config.SystemDefault;
import project.fitnessapplicationexam.exercise.form.ExerciseForm;
import project.fitnessapplicationexam.exercise.dto.ExerciseRow;
import project.fitnessapplicationexam.exercise.model.Equipment;
import project.fitnessapplicationexam.exercise.model.Exercise;
import project.fitnessapplicationexam.exercise.model.MuscleGroup;
import project.fitnessapplicationexam.exercise.repository.ExerciseRepository;
import project.fitnessapplicationexam.exercise.service.ExerciseService;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequestMapping("/exercises")
@RequiredArgsConstructor
@Controller
@Validated
public class ExerciseController {
    private final ExerciseService exerciseService;
    private final UserService users;
    private final ExerciseRepository exerciseRepo;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails me, Model model) {
        UUID ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        User u = users.findByUsernameOrThrow(me.getUsername());
        List<UUID> owners = List.of(SystemDefault.SYSTEM_USER_ID, ownerId);

        List<ExerciseRow> rows = exerciseRepo.findAllByOwnerUserIdInOrderByNameAsc(owners)
                .stream()
                .map(ex -> new ExerciseRow(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        ex.getEquipment(),
                        SystemDefault.SYSTEM_USER_ID.equals(ex.getOwnerUserId())
                ))
                .toList();

        model.addAttribute("exercises", rows);
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        model.addAttribute("form", new ExerciseForm());
        model.addAttribute("muscles", MuscleGroup.values());
        model.addAttribute("equipments", Equipment.values());
        return "exercises";
    }

    @PostMapping({"", "/", "/create", "/add"})
    public String create(@AuthenticationPrincipal UserDetails me,
                         @ModelAttribute @Validated ExerciseForm form) {
        UUID ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        Exercise exercise = Exercise.builder()
                .ownerUserId(ownerId)
                .name(form.getName())
                .primaryMuscle(form.getMuscleGroup())
                .equipment(form.getEquipment())
                .createdOn(LocalDateTime.now())
                .build();
        exerciseService.create(exercise);
        return "redirect:/exercises";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails me) {
        UUID ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        exerciseRepo.findByIdAndOwnerUserId(id, ownerId).ifPresent(exercise -> {
            exerciseService.delete(id);
        });
        return "redirect:/exercises";
    }
}
