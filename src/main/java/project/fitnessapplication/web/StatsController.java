package project.fitnessapplication.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.fitnessapplication.stats.service.AdvancedStatsService;
import project.fitnessapplication.stats.service.StatsService;
import project.fitnessapplication.user.service.UserService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Controller
public class StatsController {

    private final StatsService stats;
    private final AdvancedStatsService advancedStats;
    private final UserService users;

    public StatsController(StatsService stats, AdvancedStatsService advancedStats, UserService users) {
        this.stats = stats;
        this.advancedStats = advancedStats;
        this.users = users;
    }

    @GetMapping("/stats/weekly")
    public String weekly(@AuthenticationPrincipal UserDetails me,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         Model model) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        var u = users.findByUsernameOrThrow(me.getUsername());
        LocalDate today = LocalDate.now();
        LocalDate start = (from != null) ? from : today.with(DayOfWeek.MONDAY);
        LocalDate end = (to != null) ? to : start.plusDays(6);
        model.addAttribute("summary", stats.weekly(userId, start, end));
        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        return "stats-weekly";
    }

    @GetMapping("/stats/advanced")
    public String advanced(@AuthenticationPrincipal UserDetails me,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        var u = users.findByUsernameOrThrow(me.getUsername());
        UUID userId = u.getId();

        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(90);

        var trainingFrequency = advancedStats.getTrainingFrequency(userId, start, end);
        var volumeTrends = advancedStats.getExerciseVolumeTrends(userId, start, end);
        var progressiveOverload = advancedStats.getProgressiveOverload(userId, start, end);
        var personalRecords = advancedStats.getPersonalRecords(userId);

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("trainingFrequency", trainingFrequency);
        model.addAttribute("volumeTrends", volumeTrends);
        model.addAttribute("progressiveOverload", progressiveOverload);
        model.addAttribute("personalRecords", personalRecords);

        return "advanced-stats";
    }
}
