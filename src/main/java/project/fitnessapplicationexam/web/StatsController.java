package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.*;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.SubscriptionTier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import project.fitnessapplicationexam.config.ValidationConstants;

@Controller
@RequiredArgsConstructor
public class StatsController {

    private static final Logger log = LoggerFactory.getLogger(StatsController.class);
    private static final Set<String> AUTO_MILESTONE_TITLES = Set.of(
            "Centurion",
            "Dedicated (50 Sessions)",
            "Dedicated",
            "Getting Started",
            "Million Pound Club",
            "Half Million",
            "100K Club",
            "Consistency King",
            "Dedicated (12 in 30)"
    );

    private final AnalyticsClient analyticsClient;
    private final UserService userService;

    @GetMapping("/stats/weekly")
    public String weekly(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        UUID userId = user.getId();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);

        try {
            analyticsClient.recomputeWeeklyStats(userId, new RecomputeWeeklyRequest(start, end));
        } catch (Exception e) {
            log.warn("Failed to refresh weekly stats in analytics microservice for user {}", userId, e);
        }

        WeeklySummaryResponse summary = fetchWeeklySummary(userId, start, end);
        if (summary == null) {
            summary = new WeeklySummaryResponse(start, end, Collections.emptyList());
        }

        addCommonAttributes(model, user);
        model.addAttribute("summary", summary);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("isPro", user.getSubscriptionTier() == SubscriptionTier.PRO && user.isSubscriptionActive());
        return "stats-weekly";
    }

    @GetMapping("/stats/advanced")
    public String advanced(@AuthenticationPrincipal UserDetails me,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (me == null) {
            return "redirect:/login";
        }
        User user = userService.findByUsernameOrThrow(me.getUsername());
        if (user.getSubscriptionTier() != SubscriptionTier.PRO || !user.isSubscriptionActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Advanced Analytics is only available for PRO subscribers. Please upgrade your subscription to access this feature.");
            return "redirect:/subscription";
        }
        UUID userId = user.getId();

        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(ValidationConstants.DEFAULT_ANALYTICS_DAYS);

        TrainingFrequencyResponse trainingFrequency = null;
        List<ExerciseVolumeTrendDto> volumeTrends = null;
        List<ProgressiveOverloadDto> progressiveOverload = null;
        PersonalRecordsDto personalRecords = null;
        List<MilestoneDto> milestoneDtos = null;
        boolean microserviceAvailable = true;
        
        try {
            trainingFrequency = getBodyOrNull(analyticsClient.getTrainingFrequency(userId, start, end));
            volumeTrends = getBodyOrNull(analyticsClient.getExerciseVolumeTrends(userId, start, end));
            progressiveOverload = getBodyOrNull(analyticsClient.getProgressiveOverload(userId, start, end));
            personalRecords = getBodyOrNull(analyticsClient.getPersonalRecords(userId));
            milestoneDtos = getBodyOrNull(analyticsClient.getMilestones(userId));
        } catch (feign.FeignException e) {
            microserviceAvailable = false;
            log.error("Error calling analytics microservice (FeignException) for user {}", userId, e);
        } catch (Exception e) {
            microserviceAvailable = false;
            log.error("Error calling analytics microservice for user {}", userId, e);
        }
        
        trainingFrequency = trainingFrequency != null ? trainingFrequency : createEmptyTrainingFrequency();
        volumeTrends = volumeTrends != null ? volumeTrends : Collections.emptyList();
        progressiveOverload = progressiveOverload != null ? progressiveOverload : Collections.emptyList();
        personalRecords = personalRecords != null ? personalRecords : new PersonalRecordsDto(Collections.emptyList(), Collections.emptyList());
        milestoneDtos = milestoneDtos != null ? milestoneDtos : Collections.emptyList();

        List<MilestoneDto> manageableMilestones = milestoneDtos.stream()
                .filter(m -> !m.systemGenerated() && !AUTO_MILESTONE_TITLES.contains(m.title()))
                .toList();
        boolean hasSystemMilestones = milestoneDtos.stream()
                .anyMatch(m -> m.systemGenerated() || AUTO_MILESTONE_TITLES.contains(m.title()));

        addCommonAttributes(model, user);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("trainingFrequency", trainingFrequency);
        model.addAttribute("volumeTrends", volumeTrends);
        model.addAttribute("progressiveOverload", progressiveOverload);
        model.addAttribute("personalRecords", personalRecords);
        model.addAttribute("milestoneDtos", milestoneDtos);
        model.addAttribute("manageableMilestones", manageableMilestones);
        model.addAttribute("hasSystemMilestones", hasSystemMilestones);
        model.addAttribute("milestoneTypes", CreateMilestoneRequest.MilestoneType.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("microserviceAvailable", microserviceAvailable);

        return "advanced-stats";
    }

    @PostMapping("/stats/milestones")
    public String createMilestone(@AuthenticationPrincipal UserDetails me,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate achievedDate,
                                  @RequestParam CreateMilestoneRequest.MilestoneType type,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        if (user.getSubscriptionTier() != SubscriptionTier.PRO || !user.isSubscriptionActive()) {
            redirectAttributes.addFlashAttribute("analyticsError", "You need an active PRO subscription to manage milestones.");
            return "redirect:/stats/advanced";
        }
        UUID userId = user.getId();

        String trimmedTitle = title != null ? title.trim() : "";
        if (trimmedTitle.isEmpty()) {
            redirectAttributes.addFlashAttribute("analyticsError", "Milestone title is required.");
            return "redirect:/stats/advanced";
        }

        CreateMilestoneRequest request = new CreateMilestoneRequest(
                userId,
                trimmedTitle,
                description != null ? description.trim() : null,
                achievedDate != null ? achievedDate : LocalDate.now(),
                type
        );
        try {
            analyticsClient.createMilestone(userId, request);
            redirectAttributes.addFlashAttribute("analyticsMessage", "Milestone added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("analyticsError", "Failed to add milestone: " + e.getMessage());
        }
        return "redirect:/stats/advanced";
    }

    @PostMapping("/stats/milestones/{id}/delete")
    public String deleteMilestone(@AuthenticationPrincipal UserDetails me,
                                  @PathVariable UUID id,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.findByUsernameOrThrow(me.getUsername());
        if (user.getSubscriptionTier() != SubscriptionTier.PRO || !user.isSubscriptionActive()) {
            redirectAttributes.addFlashAttribute("analyticsError", "You need an active PRO subscription to manage milestones.");
            return "redirect:/stats/advanced";
        }
        UUID userId = user.getId();
        try {
            analyticsClient.deleteMilestone(userId, id);
            redirectAttributes.addFlashAttribute("analyticsMessage", "Milestone removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("analyticsError", "Failed to remove milestone: " + e.getMessage());
        }
        return "redirect:/stats/advanced";
    }

    private WeeklySummaryResponse fetchWeeklySummary(UUID userId, LocalDate start, LocalDate end) {
        try {
            var response = analyticsClient.getWeeklyStats(userId, start, end);
            return response != null ? response.getBody() : null;
        } catch (Exception e) {
            log.error("Error calling analytics microservice for user {}", userId, e);
            return null;
        }
    }

    private <T> T getBodyOrNull(org.springframework.http.ResponseEntity<T> response) {
        return response != null ? response.getBody() : null;
    }

    private TrainingFrequencyResponse createEmptyTrainingFrequency() {
        return new TrainingFrequencyResponse(
                0, 0.0, Collections.emptyMap(), Collections.emptyList(), 0, 0.0);
    }

    private void addCommonAttributes(Model model, User user) {
        model.addAttribute("navAvatar", user.getProfilePicture());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
    }
}
