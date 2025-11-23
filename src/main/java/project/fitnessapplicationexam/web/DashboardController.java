package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        WeeklySummaryResponse weeklySummary = fetchWeeklySummary(userId, startOfWeek, endOfWeek);
        if (weeklySummary == null) {
            weeklySummary = new WeeklySummaryResponse(startOfWeek, endOfWeek, Collections.emptyList());
        }
        
        addCommonAttributes(model, user);
        model.addAttribute("summary", weeklySummary);
        model.addAttribute("recentWorkouts", workoutService.getRecentSessions(userId, ValidationConstants.RECENT_SESSIONS_LIMIT_5));
        return "dashboard";
    }

    private WeeklySummaryResponse fetchWeeklySummary(UUID userId, LocalDate start, LocalDate end) {
        try {
            var response = analyticsClient.getWeeklyStats(userId, start, end);
            WeeklySummaryResponse summary = response != null ? response.getBody() : null;
            if (summary == null) {
                log.warn("Analytics microservice returned null response for user {}", userId);
            } else {
                int totalSessions = summary.days().stream()
                        .mapToInt(WeeklySummaryResponse.DayStat::sessions)
                        .sum();
                log.debug("Analytics data received: {} days, total sessions: {}", summary.days().size(), totalSessions);
            }
            return summary;
        } catch (Exception e) {
            log.error("Error calling analytics microservice for user {}", userId, e);
            return null;
        }
    }

    private void addCommonAttributes(Model model, User user) {
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
    }
}
