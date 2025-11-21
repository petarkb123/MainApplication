package project.fitnessapplication.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplication.user.service.UserService;
import project.fitnessapplication.user.service.UserSettingsService;
import java.util.UUID;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserSettingsService settings;
    private final UserService users;

    @GetMapping({"", "/"})
    public String settings(@AuthenticationPrincipal UserDetails me, Model model) {
        var u = settings.requireByUsername(me.getUsername());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("avatarPath", u.getProfilePicture());
        model.addAttribute("navAvatar", u.getProfilePicture());
        return "settings";
    }

    @PostMapping("/avatar-url")
    public String setAvatarUrl(@AuthenticationPrincipal UserDetails me,
                               @RequestParam("avatarUrl") String avatarUrl,
                               RedirectAttributes ra) {
        try {
            UUID id = users.findByUsernameOrThrow(me.getUsername()).getId();
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
        UUID id = users.findByUsernameOrThrow(me.getUsername()).getId();
        settings.removeAvatar(id);
        ra.addFlashAttribute("successMessage", "Profile picture removed.");
        return "redirect:/settings";
    }


    @PostMapping("/profile")
    public String updateUsername(@AuthenticationPrincipal UserDetails me,
                                 @RequestParam("username") String newUsername,
                                 RedirectAttributes ra) {
        try {
            UUID id = users.findByUsernameOrThrow(me.getUsername()).getId();
            users.changeUsername(id, newUsername);

            var u = users.findByIdOrThrow(id);
            var principal = org.springframework.security.core.userdetails.User
                    .withUsername(u.getUsername())
                    .password(u.getPasswordHash())
                    .roles(u.getRole().name())
                    .disabled(!u.isActive())
                    .build();

            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, principal.getPassword(), principal.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            ra.addFlashAttribute("successMessage", "Profile saved.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings";
    }



    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails me,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        try {
            UUID id = users.findByUsernameOrThrow(me.getUsername()).getId();
            settings.changePassword(id, currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("successMessage", "Password changed.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings";
    }
}
