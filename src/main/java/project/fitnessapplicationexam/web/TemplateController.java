package project.fitnessapplicationexam.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.template.dto.*;
import project.fitnessapplicationexam.template.form.TemplateForm;
import project.fitnessapplicationexam.template.dto.TemplateItemForEditDto;
import project.fitnessapplicationexam.template.form.TemplateItemForm;
import project.fitnessapplicationexam.template.service.TemplateService;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.template.model.WorkoutTemplate;
import project.fitnessapplicationexam.template.model.TemplateItem;
import project.fitnessapplicationexam.exercise.model.Exercise;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserService userService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID userId = user.getId();
        addCommonAttributes(model, user);
        model.addAttribute("templates", templateService.list(userId));
        return "templates";
    }

    @GetMapping("/create")
    public String createForm(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());

        List<ExerciseOptionDto> options = templateService.getAvailableExercises(user.getId())
                .stream()
                .map(ex -> new ExerciseOptionDto(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        ex.getOwnerUserId()
                ))
                .toList();

        addCommonAttributes(model, user);
        model.addAttribute("isPro", user.getSubscriptionTier() == SubscriptionTier.PRO && user.isSubscriptionActive());
        model.addAttribute("form", new TemplateForm());
        model.addAttribute("exercises", options);
        return "create";
    }

    @PostMapping({"", "/", "/create", "/add"})
    @Transactional
    public String create(@AuthenticationPrincipal UserDetails me,
                         @ModelAttribute("form") @Valid TemplateForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes ra) {

        UUID userId = userService.findByUsernameOrThrow(me.getUsername()).getId();

        if (binding.hasErrors()) {
            model.addAttribute("exercises", templateService.getAvailableExercises(userId));
            return "create";
        }

        List<TemplateItemForm> rows = (form.getItems() == null) ? new ArrayList<>() : form.getItems();
        ArrayList<TemplateItemData> items = new ArrayList<>();
        int nextPosition = 0;

        for (TemplateItemForm r : rows) {
            if (r == null) continue;

            UUID exId = r.getExerciseId();
            if (exId == null) continue;

            int sets = (r.getSets() == null || r.getSets() < 1) ? 1 : r.getSets();

            Integer oi = r.getOrderIndex();
            int position = (oi == null || oi < 0) ? nextPosition : oi;
            nextPosition = Math.max(nextPosition, position + 1);

            UUID groupId = r.getGroupId();

            items.add(new TemplateItemData(exId, sets, position, groupId, r.getGroupType(), r.getGroupOrder(), r.getSetNumber()));
        }

        templateService.createTemplate(userId, form.getName().trim(), items);

        ra.addFlashAttribute("success", "Template created.");
        return "redirect:/templates";
    }



    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable UUID id,
                         @AuthenticationPrincipal UserDetails me,
                         RedirectAttributes ra) {
        UUID ownerId = userService.findByUsernameOrThrow(me.getUsername()).getId();

        templateService.deleteTemplate(id, ownerId);

        ra.addFlashAttribute("success", "Template deleted.");
        return "redirect:/templates";
    }




    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id,
                           @AuthenticationPrincipal UserDetails me,
                           Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID ownerId = user.getId();

        WorkoutTemplate tpl = templateService.findByIdAndOwner(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<TemplateItem> items = templateService.getTemplateItems(id);
        
        List<TemplateItemForEditDto> itemsDto = items.stream()
                .map(item -> new TemplateItemForEditDto(
                        item.getId(),
                        item.getExerciseId(),
                        item.getTargetSets(),
                        item.getPosition(),
                        item.getGroupId(),
                        item.getGroupType(),
                        item.getGroupOrder(),
                        item.getSetNumber()
                ))
                .filter(dto -> dto.exerciseId() != null)
                .toList();

        List<ExerciseOptionDto> options = templateService.getAvailableExercises(ownerId)
                .stream()
                .map(ex -> new ExerciseOptionDto(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        ex.getOwnerUserId()
                ))
                .toList();

        TemplateForm form = new TemplateForm();
        form.setName(tpl.getName());

        addCommonAttributes(model, user);
        model.addAttribute("isPro", user.getSubscriptionTier() == SubscriptionTier.PRO && user.isSubscriptionActive());
        model.addAttribute("template", tpl);
        model.addAttribute("form", form);
        model.addAttribute("items", itemsDto);
        model.addAttribute("exercises", options);

        return "edit";
    }

    @PostMapping("/{id}/edit")
    @Transactional
    public String editSave(@PathVariable UUID id,
                           @AuthenticationPrincipal UserDetails me,
                           @ModelAttribute("form") @Valid TemplateForm form,
                           BindingResult binding,
                           Model model,
                           RedirectAttributes ra) {

        UUID ownerId = userService.findByUsernameOrThrow(me.getUsername()).getId();
        Optional<WorkoutTemplate> tplOpt = templateService.findByIdAndOwner(id, ownerId);
        if (tplOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Template not found or not accessible.");
            return "redirect:/templates";
        }
        WorkoutTemplate tpl = tplOpt.get();

        User user = userService.findByUsernameOrThrow(me.getUsername());
        addCommonAttributes(model, user);
        model.addAttribute("template", tpl);

        if (binding.hasErrors()) return "edit";

        String newName = form.getName().trim();
        boolean nameTaken = templateService.isNameTaken(ownerId, newName)
                && !tpl.getName().equalsIgnoreCase(newName);
        if (nameTaken) {
            binding.rejectValue("name", "duplicate", "You already have a template with that name.");
            return "edit";
        }

        List<TemplateItemForm> rows = form.getItems() == null ? new ArrayList<>() : form.getItems();
        ArrayList<TemplateItemData> items = new ArrayList<>();
        int fallbackPos = 0;
        for (TemplateItemForm r : rows) {
            if (r == null || r.getExerciseId() == null) continue;
            int sets = (r.getSets() == null || r.getSets() < 1) ? 1 : r.getSets();
            int position = (r.getOrderIndex() == null || r.getOrderIndex() < 0) ? fallbackPos : r.getOrderIndex();
            fallbackPos++;

            UUID groupId = r.getGroupId();

            items.add(new TemplateItemData(
                    r.getExerciseId(),
                    sets,
                    position,
                    groupId,
                    r.getGroupType(),
                    r.getGroupOrder(),
                    r.getSetNumber()
            ));
        }

        templateService.updateTemplate(id, ownerId, newName, items);

        ra.addFlashAttribute("success", "Template updated.");
        return "redirect:/templates";
    }



    @GetMapping("/{id}/exercises")
    @ResponseBody
    public List<ExerciseSummary> templateExercises(@PathVariable UUID id,
                                                   @AuthenticationPrincipal UserDetails me) {
        UUID ownerId = userService.findByUsernameOrThrow(me.getUsername()).getId();

        WorkoutTemplate tpl = templateService.findByIdAndOwner(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<TemplateItem> items = templateService.getTemplateItems(tpl.getId());

        Map<UUID, Exercise> exMap = templateService.getExercisesByIds(
                items.stream().map(i -> i.getExerciseId()).toList()
        );

        ArrayList<ExerciseSummary> out = new ArrayList<>(items.size());
        for (TemplateItem it : items) {
            Exercise ex = exMap.get(it.getExerciseId());
            if (ex != null) {
                out.add(new ExerciseSummary(ex.getId(), ex.getName(), ex.getPrimaryMuscle()));
            }
        }
        return out;
    }

    private void addCommonAttributes(Model model, User user) {
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
    }
}
