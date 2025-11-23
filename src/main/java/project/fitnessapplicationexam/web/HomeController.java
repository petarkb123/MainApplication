package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.user.model.User;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final WorkoutSessionRepository sessions;

    @GetMapping({"/", "/home"})
    public String index(@AuthenticationPrincipal UserDetails me, Model model) {
        if (me != null) {
            User user = userService.findByUsernameOrThrow(me.getUsername());
            model.addAttribute("navAvatar", user.getProfilePicture());
            model.addAttribute("username", user.getUsername());
            model.addAttribute("recentWorkouts", sessions.findTop5ByUserIdOrderByStartedAtDesc(user.getId()));
        }
        return "index";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
