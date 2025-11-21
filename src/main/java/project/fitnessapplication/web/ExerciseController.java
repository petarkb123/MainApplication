package project.fitnessapplication.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplication.config.SystemDefault;
import project.fitnessapplication.exercise.form.ExerciseForm;
import project.fitnessapplication.exercise.dto.ExerciseRow;
import project.fitnessapplication.exercise.model.Equipment;
import project.fitnessapplication.exercise.model.Exercise;
import project.fitnessapplication.exercise.model.MuscleGroup;
import project.fitnessapplication.exercise.repository.ExerciseRepository;
import project.fitnessapplication.user.service.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

@RequestMapping("/exercises")
@RequiredArgsConstructor
@Controller
@Validated
public class ExerciseController {
    private final ExerciseRepository exerciseRepo;
    private final UserService users;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails me, Model model) {
        var ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        var u = users.findByUsernameOrThrow(me.getUsername());
        var owners = java.util.List.of(SystemDefault.SYSTEM_USER_ID, ownerId);

        var rows = exerciseRepo.findAllByOwnerUserIdInOrderByNameAsc(owners)
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
        model.addAttribute("form", new ExerciseForm());
        model.addAttribute("muscles", MuscleGroup.values());
        model.addAttribute("equipments", Equipment.values());
        return "exercises/list";
    }

    @PostMapping({"", "/", "/create", "/add"})
    public String create(@AuthenticationPrincipal UserDetails me,
                         @ModelAttribute @Validated ExerciseForm form) {
        var ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        exerciseRepo.save(Exercise.builder()
                .ownerUserId(ownerId)
                .name(form.getName())
                .primaryMuscle(form.getMuscleGroup())
                .equipment(form.getEquipment())
                .createdOn(LocalDateTime.now())
                .build());
        return "redirect:/exercises";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails me) {
        var ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        exerciseRepo.findByIdAndOwnerUserId(id, ownerId).ifPresent(exerciseRepo::delete);
        return "redirect:/exercises";
    }
}
