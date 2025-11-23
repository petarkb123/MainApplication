package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSubscriptionService;

import java.util.UUID;

@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final UserSubscriptionService subscriptions;
    private final UserService userService;

    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("currentTier", user.getSubscriptionTier());
        model.addAttribute("subscriptionActive", user.isSubscriptionActive());
        model.addAttribute("nextRenewalAt", user.getNextRenewalAt());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
        return "subscription";
    }

    @PostMapping("/select")
    public String select(@AuthenticationPrincipal UserDetails me,
                         @RequestParam("tier") SubscriptionTier tier,
                         RedirectAttributes ra) {
        UUID id = userService.findByUsernameOrThrow(me.getUsername()).getId();
        if (tier == SubscriptionTier.BASIC) {
            subscriptions.activateBasic(id);
            ra.addFlashAttribute("successMessage", "You are now on the Basic plan.");
        } else {
            subscriptions.activatePro(id);
            ra.addFlashAttribute("successMessage", "You are now on the Pro plan. Enjoy advanced methods!");
        }
        return "redirect:/subscription";
    }
}

