package project.fitnessapplication.web;

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
import project.fitnessapplication.template.dto.*;
import project.fitnessapplication.template.form.TemplateForm;
import project.fitnessapplication.template.form.TemplateItemForm;
import project.fitnessapplication.template.service.TemplateService;
import project.fitnessapplication.user.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserService users;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails me, Model model) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        var u = users.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("templates", templateService.list(userId));
        return "templates/list";
    }

    @GetMapping("/create")
    public String createForm(@AuthenticationPrincipal UserDetails me, Model model) {
        var u = users.findByUsernameOrThrow(me.getUsername());

        var options = templateService.getAvailableExercises(u.getId())
                .stream()
                .map(ex -> new ExerciseOptionDto(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        ex.getOwnerUserId()
                ))
                .toList();

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("form", new TemplateForm());
        model.addAttribute("exercises", options);   // <-- used by the page JS
        return "templates/create";
    }

    @PostMapping({"", "/", "/create", "/add"})
    @org.springframework.transaction.annotation.Transactional
    public String create(@AuthenticationPrincipal UserDetails me,
                         @ModelAttribute("form") @Valid TemplateForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes ra) {

        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();

        if (binding.hasErrors()) {
            model.addAttribute("exercises", templateService.getAvailableExercises(userId));
            return "templates/create";
        }

        List<TemplateItemForm> rows = (form.getItems() == null) ? new ArrayList<>() : form.getItems();
        var items = new java.util.ArrayList<TemplateItemData>();
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
    @org.springframework.transaction.annotation.Transactional
    public String delete(@PathVariable UUID id,
                         @AuthenticationPrincipal UserDetails me,
                         RedirectAttributes ra) {
        UUID ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();

        templateService.deleteTemplate(id, ownerId);

        ra.addFlashAttribute("success", "Template deleted.");
        return "redirect:/templates";
    }




    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id,
                           @AuthenticationPrincipal UserDetails me,
                           Model model) {
        var u = users.findByUsernameOrThrow(me.getUsername());
        var ownerId = u.getId();

        var tpl = templateService.findByIdAndOwner(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var items = templateService.getTemplateItems(id);

        var options = templateService.getAvailableExercises(ownerId)
                .stream()
                .map(ex -> new ExerciseOptionDto(
                        ex.getId(),
                        ex.getName(),
                        ex.getPrimaryMuscle(),
                        ex.getOwnerUserId()
                ))
                .toList();

        var form = new TemplateForm();
        form.setName(tpl.getName());

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("template", tpl);
        model.addAttribute("form", form);
        model.addAttribute("items", items);      
        model.addAttribute("exercises", options);

        return "templates/edit"; // your edit page view name
    }

    @PostMapping("/{id}/edit")
    @org.springframework.transaction.annotation.Transactional
    public String editSave(@PathVariable UUID id,
                           @AuthenticationPrincipal UserDetails me,
                           @ModelAttribute("form") @Valid TemplateForm form,
                           BindingResult binding,
                           Model model,
                           RedirectAttributes ra) {

        UUID ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();
        var tplOpt = templateService.findByIdAndOwner(id, ownerId);
        if (tplOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Template not found or not accessible.");
            return "redirect:/templates";
        }
        var tpl = tplOpt.get();

        var u = users.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("template", tpl);

        if (binding.hasErrors()) return "templates/edit";

        var newName = form.getName().trim();
        boolean nameTaken = templateService.isNameTaken(ownerId, newName)
                && !tpl.getName().equalsIgnoreCase(newName);
        if (nameTaken) {
            binding.rejectValue("name", "duplicate", "You already have a template with that name.");
            return "templates/edit";
        }

        List<TemplateItemForm> rows = form.getItems() == null ? new ArrayList<>() : form.getItems();
        var items = new java.util.ArrayList<TemplateItemData>();
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
        UUID ownerId = users.findByUsernameOrThrow(me.getUsername()).getId();

        var tpl = templateService.findByIdAndOwner(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var items = templateService.getTemplateItems(tpl.getId());

        var exMap = templateService.getExercisesByIds(
                items.stream().map(i -> i.getExerciseId()).toList()
        );

        var out = new ArrayList<ExerciseSummary>(items.size());
        for (var it : items) {
            var ex = exMap.get(it.getExerciseId());
            if (ex != null) {
                out.add(new ExerciseSummary(ex.getId(), ex.getName(), ex.getPrimaryMuscle()));
            }
        }
        return out;
    }
}
