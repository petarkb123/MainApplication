package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSettingsService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import java.util.UUID;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserSettingsService settings;
    private final UserService userService;

    @GetMapping({"", "/"})
    public String settings(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = settings.requireByUsername(me.getUsername());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("avatarPath", user.getProfilePicture());
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
        return "settings";
    }

    @PostMapping("/avatar-url")
    public String setAvatarUrl(@AuthenticationPrincipal UserDetails me,
                               @RequestParam("avatarUrl") String avatarUrl,
                               RedirectAttributes ra) {
        try {
            UUID id = userService.findByUsernameOrThrow(me.getUsername()).getId();
            settings.setAvatarUrl(id, avatarUrl);
            ra.addFlashAttribute("successMessage", "Profile picture updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/avatar/delete")
    public String deleteAvatar(@AuthenticationPrincipal UserDetails me,
                               RedirectAttributes ra) {
        UUID id = userService.findByUsernameOrThrow(me.getUsername()).getId();
        settings.removeAvatar(id);
        ra.addFlashAttribute("successMessage", "Profile picture removed.");
        return "redirect:/settings";
    }


    @PostMapping("/profile")
    public String updateUsername(@AuthenticationPrincipal UserDetails me,
                                 @RequestParam("username") String newUsername,
                                 RedirectAttributes ra) {
        try {
            UUID userId = userService.findByUsernameOrThrow(me.getUsername()).getId();
            userService.changeUsername(userId, newUsername);
            updateSecurityContext(userId);
            ra.addFlashAttribute("successMessage", "Profile saved.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings";
    }

    private void updateSecurityContext(UUID userId) {
        User user = userService.findByIdOrThrow(userId);
        org.springframework.security.core.userdetails.UserDetails principal =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .disabled(!user.isActive())
                        .build();

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, principal.getPassword(), principal.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }



    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails me,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        try {
            UUID userId = userService.findByUsernameOrThrow(me.getUsername()).getId();
            settings.changePassword(userId, currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("successMessage", "Password changed.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings";
    }
}
