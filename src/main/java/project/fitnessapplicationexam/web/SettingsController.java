package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.service.UserSettingsService;
import java.util.UUID;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
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
        UUID id = userService.findByUsernameOrThrow(me.getUsername()).getId();
        settings.setAvatarUrl(id, avatarUrl);
        ra.addFlashAttribute("successMessage", "Profile picture updated.");
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
        UUID userId = userService.findByUsernameOrThrow(me.getUsername()).getId();
        userService.changeUsername(userId, newUsername);
        updateSecurityContext(userId);
        ra.addFlashAttribute("successMessage", "Profile saved.");
        return "redirect:/settings";
    }

    private void updateSecurityContext(UUID userId) {
        project.fitnessapplicationexam.user.model.User user = userService.findByIdOrThrow(userId);
        UserDetails principal = org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .disabled(!user.isActive())
                        .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, principal.getPassword(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }



    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails me,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        UUID userId = userService.findByUsernameOrThrow(me.getUsername()).getId();
        settings.changePassword(userId, currentPassword, newPassword, confirmPassword);
        ra.addFlashAttribute("successMessage", "Password changed.");
        return "redirect:/settings";
    }
}
