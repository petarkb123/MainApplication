package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final UserRepository userRepository;
    private final UserSubscriptionService userSubscriptionService;
    private final UserService userService;

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal UserDetails me, Model model) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", currentUser.getProfilePicture());
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("isAdmin", currentUser.getRole() == UserRole.ADMIN);
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("users", userRepository.findAll());
        return "admin-users";
    }

    @PostMapping("/users/{id}/deactivate-account")
    public String deactivateAccount(@PathVariable UUID id,
                                   @AuthenticationPrincipal UserDetails me,
                                   RedirectAttributes ra) {
        if (isSelfOperation(id, me, ra, "You cannot deactivate your own account.")) {
            return "redirect:/admin/users";
        }
        
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
            log.info("Account deactivated by admin: user {}", id);
        });
        ra.addFlashAttribute("successMessage", "Account deactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate-account")
    public String activateAccount(@PathVariable UUID id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(true);
            userRepository.save(user);
            log.info("Account activated by admin: user {}", id);
        });
        ra.addFlashAttribute("successMessage", "Account activated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/deactivate-subscription")
    public String deactivateSubscription(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserDetails me,
                                        RedirectAttributes ra) {
        if (isSelfOperation(id, me, ra, "You cannot deactivate your own subscription.")) {
            return "redirect:/admin/users";
        }
        
        userSubscriptionService.deactivateSubscription(id);
        ra.addFlashAttribute("successMessage", "Subscription deactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-subscription")
    public String toggleSubscription(@PathVariable UUID id,
                                   @AuthenticationPrincipal UserDetails me,
                                   RedirectAttributes ra) {
        if (isSelfOperation(id, me, ra, "You cannot modify your own subscription from here. Please use the subscription page.")) {
            return "redirect:/admin/users";
        }
        
        User user = userRepository.findById(id).orElseThrow();
        if (user.getSubscriptionTier() == SubscriptionTier.PRO) {
            userSubscriptionService.activateBasic(id);
            ra.addFlashAttribute("successMessage", "Subscription changed to BASIC.");
        } else {
            userSubscriptionService.activatePro(id);
            ra.addFlashAttribute("successMessage", "Subscription changed to PRO.");
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/resume-subscription")
    public String resumeSubscription(@PathVariable UUID id,
                                    @AuthenticationPrincipal UserDetails me,
                                    RedirectAttributes ra) {
        if (isSelfOperation(id, me, ra, "You cannot resume your own subscription from here. Please use the subscription page.")) {
            return "redirect:/admin/users";
        }
        
        User user = userRepository.findById(id).orElseThrow();
        if (user.getSubscriptionTier() == SubscriptionTier.PRO) {
            userSubscriptionService.activatePro(id);
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

        userRepository.findById(id).ifPresent(user -> {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
            log.info("User {} promoted to admin by {}", id, currentUser.getUsername());
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
        
        userRepository.findById(id).ifPresent(user -> {
            user.setRole(UserRole.USER);
            userRepository.save(user);
            log.info("Admin status removed from user {} by {}", id, currentUser.getUsername());
        });
        ra.addFlashAttribute("successMessage", "Admin status removed.");
        return "redirect:/admin/users";
    }

    private boolean isSelfOperation(UUID targetId, UserDetails me, RedirectAttributes ra, String errorMessage) {
        User currentUser = userService.findByUsernameOrThrow(me.getUsername());
        if (currentUser.getId().equals(targetId)) {
            ra.addFlashAttribute("errorMessage", errorMessage);
            return true;
        }
        return false;
    }
}

