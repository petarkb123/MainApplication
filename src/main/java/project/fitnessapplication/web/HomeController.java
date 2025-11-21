package project.fitnessapplication.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.fitnessapplication.user.service.UserService;
import project.fitnessapplication.workout.repository.WorkoutSessionRepository;

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
            var u = users.findByUsernameOrThrow(me.getUsername());
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
