package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.repository.UserRepository;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSubscriptionService;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository users;
    private final UserSubscriptionService subs;
    private final UserService userService;

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal UserDetails me, Model model) {
        User u = userService.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        model.addAttribute("currentUserId", u.getId()); 
        model.addAttribute("users", users.findAll());
        return "admin-users";
    }

    @PostMapping("/users/{id}/deactivate-account")
    public String deactivateAccount(@PathVariable UUID id, 
                                   @AuthenticationPrincipal UserDetails me,
                                   RedirectAttributes ra) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "You cannot deactivate your own account.");
            return "redirect:/admin/users";
        }
        
        users.findById(id).ifPresent(u -> { u.setActive(false); users.save(u); });
        ra.addFlashAttribute("successMessage", "Account deactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate-account")
    public String activateAccount(@PathVariable UUID id, RedirectAttributes ra) {
        users.findById(id).ifPresent(u -> { u.setActive(true); users.save(u); });
        ra.addFlashAttribute("successMessage", "Account activated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/deactivate-subscription")
    public String deactivateSubscription(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserDetails me,
                                        RedirectAttributes ra) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "You cannot deactivate your own subscription.");
            return "redirect:/admin/users";
        }
        
        subs.deactivateSubscription(id);
        ra.addFlashAttribute("successMessage", "Subscription deactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-subscription")
    public String toggleSubscription(@PathVariable UUID id,
                                   @AuthenticationPrincipal UserDetails me,
                                   RedirectAttributes ra) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "You cannot modify your own subscription from here. Please use the subscription page.");
            return "redirect:/admin/users";
        }
        
        User user = users.findById(id).orElseThrow();
        
        if (user.getSubscriptionTier() == SubscriptionTier.PRO) {
            subs.activateBasic(id);
            ra.addFlashAttribute("successMessage", "Subscription changed to BASIC.");
        } else {
            subs.activatePro(id);
            ra.addFlashAttribute("successMessage", "Subscription changed to PRO.");
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/resume-subscription")
    public String resumeSubscription(@PathVariable UUID id,
                                    @AuthenticationPrincipal UserDetails me,
                                    RedirectAttributes ra) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "You cannot resume your own subscription from here. Please use the subscription page.");
            return "redirect:/admin/users";
        }
        
        User user = users.findById(id).orElseThrow();
        if (user.getSubscriptionTier() == SubscriptionTier.PRO) {
            subs.activatePro(id);
            ra.addFlashAttribute("successMessage", "Pro subscription resumed.");
        } else {
            ra.addFlashAttribute("errorMessage", "Only Pro subscriptions can be resumed.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/make-admin")
    public String makeAdmin(@PathVariable UUID id,
                           @AuthenticationPrincipal UserDetails me,
                           RedirectAttributes ra) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());

        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "You are already an admin.");
            return "redirect:/admin/users";
        }

        users.findById(id).ifPresent(u -> {
            u.setRole(UserRole.ADMIN);
            users.save(u);
        });
        ra.addFlashAttribute("successMessage", "User promoted to admin.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/remove-admin")
    public String removeAdmin(@PathVariable UUID id,
                            @AuthenticationPrincipal UserDetails me,
                            RedirectAttributes ra) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        
        if (currentUser.getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "You cannot remove admin status from yourself.");
            return "redirect:/admin/users";
        }
        
        users.findById(id).ifPresent(u -> {
            u.setRole(UserRole.USER);
            users.save(u);
        });
        ra.addFlashAttribute("successMessage", "Admin status removed.");
        return "redirect:/admin/users";
    }
}


