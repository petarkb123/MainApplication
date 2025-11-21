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
import project.fitnessapplicationexam.template.model.TemplateItem;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;
    private final TemplateService templateService;
    private final UserService users;


    @GetMapping
    public String history(@AuthenticationPrincipal UserDetails me, Model model) {
        User u = users.findByUsernameOrThrow(me.getUsername());
        UUID userId = u.getId();

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        model.addAttribute("sessions", workoutService.getRecentSessions(userId, 50));
        model.addAttribute("templates", templateService.list(userId));

        return "history";
    }

    @GetMapping("/session")
    public String session(@AuthenticationPrincipal UserDetails me,
                          @RequestParam(required = false) UUID sessionId,
                          Model model) {
        User u = users.findByUsernameOrThrow(me.getUsername());
        UUID userId = u.getId();

        WorkoutSession s = (sessionId != null)
                ? workoutService.findById(sessionId).orElseThrow()
                : workoutService.start(userId);

        if (!Objects.equals(s.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        model.addAttribute("isPro", u.getSubscriptionTier() == SubscriptionTier.PRO && u.isSubscriptionActive());
        model.addAttribute("sessionId", s.getId());
        model.addAttribute("startedAt", s.getStartedAt());

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
            model.addAttribute("existingSets", workoutService.getSessionSets(s.getId()));
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

        User u = users.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);

        Optional<WorkoutSession> opt = workoutService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Workout not found.");
            return "redirect:/workouts";
        }
        WorkoutSession session = opt.get();
        if (!Objects.equals(session.getUserId(), u.getId())) {
            ra.addFlashAttribute("error", "Workout not found.");
            return "redirect:/workouts";
        }

        List<WorkoutSet> sets = workoutService.getSessionSets(id);
        
        List<UUID> exIds = sets.stream()
                .map(WorkoutSet::getExerciseId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, Exercise> exercises = workoutService.getExercisesByIds(exIds);

        
        ArrayList<ExerciseBlock> blocks = new ArrayList<>();
        UUID lastExerciseId = null;
        List<WorkoutSet> currentBlock = new ArrayList<>();
        
        for (WorkoutSet set : sets) {
            if (lastExerciseId == null || !lastExerciseId.equals(set.getExerciseId())) {
                
                if (lastExerciseId != null && !currentBlock.isEmpty()) {
                    Exercise ex = exercises.get(lastExerciseId);
                    if (ex != null) {
                        blocks.add(new ExerciseBlock(ex, new ArrayList<>(currentBlock)));
                    }
                    currentBlock.clear();
                }
                lastExerciseId = set.getExerciseId();
            }
            currentBlock.add(set);
        }
        
        
        if (lastExerciseId != null && !currentBlock.isEmpty()) {
            Exercise ex = exercises.get(lastExerciseId);
            if (ex != null) {
                blocks.add(new ExerciseBlock(ex, currentBlock));
            }
        }

        WorkoutView workoutView = new WorkoutView(session.getStartedAt(), session.getFinishedAt(), blocks);
        model.addAttribute("workout", workoutView);
        model.addAttribute("totalSets", sets.size());

        return "details";
    }


    @RequestMapping(value = "/{id}/finish", method = {RequestMethod.GET, RequestMethod.POST})
    public String finishQuick(@PathVariable UUID id, @AuthenticationPrincipal UserDetails me) {
        User u = users.findByUsernameOrThrow(me.getUsername());
        try {
            workoutService.finishSessionWithSets(id, u.getId(), null);
        } catch (Exception e) {
            workoutService.finishSession(id, u.getId());
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

        User u = users.findByUsernameOrThrow(me.getUsername());
        
        
        List<ExerciseSetData> exerciseSets = null;
        if (body.getExercises() != null && !body.getExercises().isEmpty()) {
            exerciseSets = body.getExercises().stream()
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
        
        try {
            workoutService.finishSessionWithSets(body.getSessionId(), u.getId(), exerciseSets);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found");
            }
            return ResponseEntity.badRequest().body(e.getReason());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid workout data: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }


    @GetMapping("/templates/{templateId}/exercises")
    @ResponseBody
    public List<ExerciseOption> templateExercises(@PathVariable UUID templateId,
                                                  @AuthenticationPrincipal UserDetails me) {
        User u = users.findByUsernameOrThrow(me.getUsername());
        UUID ownerId = u.getId();

        templateService.findByIdAndOwner(templateId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<TemplateItem> items = templateService.getTemplateItems(templateId);

        List<UUID> exerciseIds = items.stream().map(it -> it.getExerciseId()).toList();
        Map<UUID, Exercise> byId = templateService.getExercisesByIds(exerciseIds);

        ArrayList<ExerciseOption> out = new ArrayList<>(items.size());
        for (TemplateItem it : items) {
            Exercise ex = byId.get(it.getExerciseId());
            if (ex != null) {
                out.add(new ExerciseOption(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        it.getTargetSets(),
                        it.getGroupId(),
                        it.getGroupType(),
                        it.getGroupOrder(),
                        it.getPosition(),
                        it.getSetNumber()
                ));
            }
        }
        return out;
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteWorkout(@PathVariable UUID id,
                                @AuthenticationPrincipal UserDetails me,
                                RedirectAttributes ra) {
        User u = users.findByUsernameOrThrow(me.getUsername());

        try {
            workoutService.deleteSession(id, u.getId());
            ra.addFlashAttribute("success", "Workout deleted.");
        } catch (ResponseStatusException e) {
            ra.addFlashAttribute("error", "Workout not found.");
        }

        return "redirect:/workouts";
    }
}
