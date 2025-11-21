package project.fitnessapplication.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.fitnessapplication.restday.service.RestDayService;
import project.fitnessapplication.stats.service.StatsService;
import project.fitnessapplication.template.service.TemplateService;
import project.fitnessapplication.user.service.UserService;
import project.fitnessapplication.workout.service.WorkoutService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService users;
    private final StatsService stats;
    private final WorkoutService workoutService;
    private final RestDayService restDayService;
    private final TemplateService templateService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails me, Model model) {
        var u = users.findByUsernameOrThrow(me.getUsername());
        UUID userId = u.getId();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        var weeklySummary = stats.weekly(userId, startOfWeek, endOfWeek);
        var recentRestDays = restDayService.getRestDaysInRange(userId, today.minusDays(30), today.plusDays(60));

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("summary", weeklySummary);
        model.addAttribute("recentWorkouts", workoutService.getRecentSessions(userId, 5));
        model.addAttribute("restDays", recentRestDays);
        model.addAttribute("templates", templateService.list(userId));
        return "dashboard";
    }
}
