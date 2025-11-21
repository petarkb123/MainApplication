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
    private final UserService users;

    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails me, Model model) {
        User u = users.findByUsernameOrThrow(me.getUsername());
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("currentTier", u.getSubscriptionTier());
        model.addAttribute("subscriptionActive", u.isSubscriptionActive());
        model.addAttribute("nextRenewalAt", u.getNextRenewalAt());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        return "subscription";
    }

    @PostMapping("/select")
    public String select(@AuthenticationPrincipal UserDetails me,
                         @RequestParam("tier") SubscriptionTier tier,
                         RedirectAttributes ra) {
        UUID id = users.findByUsernameOrThrow(me.getUsername()).getId();
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


