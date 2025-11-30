package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.RecomputeWeeklyRequest;
import project.fitnessapplicationexam.analytics.dto.WeeklySummaryResponse;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.workout.service.WorkoutService;
import project.fitnessapplicationexam.config.ValidationConstants;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final UserService userService;
    private final AnalyticsClient analyticsClient;
    private final WorkoutService workoutService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        try {
            analyticsClient.recomputeWeeklyStats(userId, new RecomputeWeeklyRequest(startOfWeek, endOfWeek));
        } catch (Exception e) {
            log.warn("Failed to refresh weekly stats in analytics microservice for user {}", userId, e);
        }

        WeeklySummaryResponse weeklySummary;
        try {
            ResponseEntity<WeeklySummaryResponse> response = analyticsClient.getWeeklyStats(userId, startOfWeek, endOfWeek);
            weeklySummary = response != null && response.getBody() != null 
                    ? response.getBody() 
                    : new WeeklySummaryResponse(startOfWeek, endOfWeek, Collections.emptyList());
        } catch (Exception e) {
            log.error("Error calling analytics microservice for user {}", userId, e);
            weeklySummary = new WeeklySummaryResponse(startOfWeek, endOfWeek, Collections.emptyList());
        }
        
        addCommonAttributes(model, user);
        model.addAttribute("summary", weeklySummary);
        model.addAttribute("recentWorkouts", workoutService.getRecentSessions(userId, ValidationConstants.RECENT_SESSIONS_LIMIT_5));
        return "dashboard";
    }

    private void addCommonAttributes(Model model, User user) {
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
    }
}
