package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.WeeklySummaryResponse;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.workout.service.WorkoutService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService users;
    private final AnalyticsClient analyticsClient;
    private final WorkoutService workoutService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails me, Model model) {
        User u = users.findByUsernameOrThrow(me.getUsername());
        UUID userId = u.getId();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        WeeklySummaryResponse weeklySummary = null;
        try {
            var response = analyticsClient.getWeeklyStats(userId, startOfWeek, endOfWeek);
            weeklySummary = response != null ? response.getBody() : null;
            if (weeklySummary == null) {
                System.err.println("Warning: Analytics microservice returned null response");
            } else {
                System.out.println("Analytics data received: " + weeklySummary.days().size() + " days, total sessions: " + 
                    weeklySummary.days().stream().mapToInt(WeeklySummaryResponse.DayStat::sessions).sum());
            }
        } catch (Exception e) {
            System.err.println("Error calling analytics microservice: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (weeklySummary == null) {
            weeklySummary = new WeeklySummaryResponse(startOfWeek, endOfWeek, java.util.Collections.emptyList());
        }
        
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        model.addAttribute("summary", weeklySummary);
        model.addAttribute("recentWorkouts", workoutService.getRecentSessions(userId, 5));
        return "dashboard";
    }
}
