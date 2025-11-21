package project.fitnessapplicationexam.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.workout.repository.WorkoutSessionRepository;
import project.fitnessapplicationexam.user.model.User;

@Controller
public class HomeController {

    private final UserService users;
    private final WorkoutSessionRepository sessions;

    public HomeController(UserService users, WorkoutSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @GetMapping({"/", "/home"})
    public String index(Authentication auth, @AuthenticationPrincipal UserDetails me, Model model) {
        if (auth != null && auth.isAuthenticated() && me != null) {
            User u = users.findByUsernameOrThrow(me.getUsername());
            model.addAttribute("navAvatar", u.getProfilePicture());
            model.addAttribute("username", u.getUsername());
            model.addAttribute("recentWorkouts", sessions.findTop5ByUserIdOrderByStartedAtDesc(u.getId()));
        }
        return "index";
    }

    @GetMapping("/terms")
    public String terms() { return "terms"; }

    @GetMapping("/privacy")
    public String privacy() { return "privacy"; }
}
