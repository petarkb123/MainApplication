package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.config.SystemDefault;
import project.fitnessapplicationexam.template.service.TemplateService;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.workout.dto.FinishWorkoutRequest;
import project.fitnessapplicationexam.workout.dto.ExerciseSetData;
import project.fitnessapplicationexam.workout.dto.ExercisePayload;
import project.fitnessapplicationexam.workout.dto.SetData;
import project.fitnessapplicationexam.workout.dto.ExerciseBlock;
import project.fitnessapplicationexam.workout.dto.WorkoutView;
import project.fitnessapplicationexam.workout.model.WorkoutSet;
import project.fitnessapplicationexam.workout.model.WorkoutSession;
import project.fitnessapplicationexam.exercise.dto.ExerciseSelect;
import project.fitnessapplicationexam.template.dto.ExerciseOption;
import project.fitnessapplicationexam.workout.service.WorkoutService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.exercise.model.Exercise;
import org.springframework.dao.DataIntegrityViolationException;
import project.fitnessapplicationexam.template.model.TemplateItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import project.fitnessapplicationexam.config.ValidationConstants;

@Controller
@RequestMapping("/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;
    private final TemplateService templateService;
    private final UserService userService;


    @GetMapping
    public String history(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID userId = user.getId();

        addCommonAttributes(model, user);
        model.addAttribute("sessions", workoutService.getRecentSessions(userId, ValidationConstants.RECENT_SESSIONS_LIMIT_50));
        model.addAttribute("templates", templateService.list(userId));

        return "history";
    }

    @GetMapping("/session")
    public String session(@AuthenticationPrincipal UserDetails me,
                          @RequestParam(required = false) UUID sessionId,
                          Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID userId = user.getId();

        WorkoutSession session = (sessionId != null)
                ? workoutService.findById(sessionId).orElseThrow()
                : workoutService.start(userId);

        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        addCommonAttributes(model, user);
        model.addAttribute("isPro", user.getSubscriptionTier() == SubscriptionTier.PRO && user.isSubscriptionActive());
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("startedAt", session.getStartedAt());

        List<ExerciseSelect> options = workoutService.getAvailableExercises(userId)
                .stream()
                .map(ex -> new ExerciseSelect(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        SystemDefault.SYSTEM_USER_ID.equals(ex.getOwnerUserId())
                ))
                .toList();

        model.addAttribute("exercises", options);

        if (sessionId != null) {
            model.addAttribute("existingSets", workoutService.getSessionSets(session.getId()));
        } else {
            model.addAttribute("existingSets", Collections.emptyList());
        }
        return "session";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable UUID id,
                          @AuthenticationPrincipal UserDetails me,
                          Model model,
                          RedirectAttributes ra) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        addCommonAttributes(model, user);

        WorkoutSession session = workoutService.findById(id)
                .filter(s -> Objects.equals(s.getUserId(), user.getId()))
                .orElse(null);

        if (session == null) {
            ra.addFlashAttribute("error", "Workout not found.");
            return "redirect:/workouts";
        }

        List<WorkoutSet> sets = workoutService.getSessionSets(id);
        List<UUID> exerciseIds = sets.stream()
                .map(WorkoutSet::getExerciseId)
                .distinct()
                .toList();
        Map<UUID, Exercise> exercises = workoutService.getExercisesByIds(exerciseIds);
        List<ExerciseBlock> blocks = buildExerciseBlocks(sets, exercises);

        WorkoutView workoutView = new WorkoutView(session.getStartedAt(), session.getFinishedAt(), blocks);
        model.addAttribute("workout", workoutView);
        model.addAttribute("totalSets", sets.size());

        return "details";
    }


    @RequestMapping(value = "/{id}/finish", method = {RequestMethod.GET, RequestMethod.POST})
    public String finishQuick(@PathVariable UUID id, @AuthenticationPrincipal UserDetails me) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        try {
            workoutService.finishSessionWithSets(id, user.getId(), null);
        } catch (Exception e) {
            workoutService.finishSession(id, user.getId());
        }
        return "redirect:/workouts";
    }

    @PostMapping(value = "/finish", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Transactional
    public ResponseEntity<?> finishRich(@AuthenticationPrincipal UserDetails me,
                                        @RequestBody FinishWorkoutRequest body) {
        if (body == null || body.getSessionId() == null) {
            return ResponseEntity.badRequest().body("Missing sessionId");
        }

        User user = userService.findByUsernameOrThrow(me.getUsername());
        List<ExerciseSetData> exerciseSets = mapToExerciseSetData(body.getExercises());
        
        try {
            workoutService.finishSessionWithSets(body.getSessionId(), user.getId(), exerciseSets);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found");
            }
            return ResponseEntity.badRequest().body(e.getReason());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid workout data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }


    @GetMapping("/templates/{templateId}/exercises")
    @ResponseBody
    public List<ExerciseOption> templateExercises(@PathVariable UUID templateId,
                                                  @AuthenticationPrincipal UserDetails me) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID ownerId = user.getId();

        templateService.findByIdAndOwner(templateId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<TemplateItem> items = templateService.getTemplateItems(templateId);
        List<UUID> exerciseIds = items.stream()
                .map(TemplateItem::getExerciseId)
                .toList();
        Map<UUID, Exercise> exercises = templateService.getExercisesByIds(exerciseIds);

        return items.stream()
                .map(item -> {
                    Exercise exercise = exercises.get(item.getExerciseId());
                    return exercise != null ? new ExerciseOption(
                            exercise.getId(),
                            exercise.getName(),
                            exercise.getPrimaryMuscle(),
                            item.getTargetSets(),
                            item.getGroupId(),
                            item.getGroupType(),
                            item.getGroupOrder(),
                            item.getPosition(),
                            item.getSetNumber()
                    ) : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteWorkout(@PathVariable UUID id,
                                @AuthenticationPrincipal UserDetails me,
                                RedirectAttributes ra) {
        User user = userService.findByUsernameOrThrow(me.getUsername());

        try {
            workoutService.deleteSession(id, user.getId());
            ra.addFlashAttribute("success", "Workout deleted.");
        } catch (ResponseStatusException e) {
            ra.addFlashAttribute("error", "Workout not found.");
        }

        return "redirect:/workouts";
    }

    private void addCommonAttributes(Model model, User user) {
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
    }

    private List<ExerciseBlock> buildExerciseBlocks(List<WorkoutSet> sets, Map<UUID, Exercise> exercises) {
        List<ExerciseBlock> blocks = new ArrayList<>();
        UUID lastExerciseId = null;
        List<WorkoutSet> currentBlock = new ArrayList<>();
        
        for (WorkoutSet set : sets) {
            if (lastExerciseId == null || !lastExerciseId.equals(set.getExerciseId())) {
                if (lastExerciseId != null && !currentBlock.isEmpty()) {
                    Exercise exercise = exercises.get(lastExerciseId);
                    if (exercise != null) {
                        blocks.add(new ExerciseBlock(exercise, new ArrayList<>(currentBlock)));
                    }
                    currentBlock.clear();
                }
                lastExerciseId = set.getExerciseId();
            }
            currentBlock.add(set);
        }
        
        if (lastExerciseId != null && !currentBlock.isEmpty()) {
            Exercise exercise = exercises.get(lastExerciseId);
            if (exercise != null) {
                blocks.add(new ExerciseBlock(exercise, currentBlock));
            }
        }
        
        return blocks;
    }

    private List<ExerciseSetData> mapToExerciseSetData(List<ExercisePayload> exercises) {
        if (exercises == null || exercises.isEmpty()) {
            return null;
        }
        return exercises.stream()
                .filter(ex -> ex != null && ex.getExerciseId() != null)
                .map(ex -> new ExerciseSetData(
                        ex.getExerciseId(),
                        ex.getSets() == null ? List.of() : ex.getSets().stream()
                                .filter(set -> set != null)
                                .map(set -> new SetData(
                                        set.getWeight(),
                                        set.getReps(),
                                        set.getGroupId(),
                                        set.getGroupType(),
                                        set.getGroupOrder(),
                                        set.getSetNumber()
                                ))
                                .toList()
                ))
                .toList();
    }
}
