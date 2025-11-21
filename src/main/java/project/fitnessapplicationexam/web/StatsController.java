package project.fitnessapplicationexam.web;

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
import project.fitnessapplicationexam.analytics.dto.CreateMilestoneRequest;
import project.fitnessapplicationexam.analytics.dto.ExerciseVolumeTrendDto;
import project.fitnessapplicationexam.analytics.dto.MilestoneDto;
import project.fitnessapplicationexam.analytics.dto.PersonalRecordsDto;
import project.fitnessapplicationexam.analytics.dto.ProgressiveOverloadDto;
import project.fitnessapplicationexam.analytics.dto.RecomputeWeeklyRequest;
import project.fitnessapplicationexam.analytics.dto.TrainingFrequencyResponse;
import project.fitnessapplicationexam.analytics.dto.WeeklySummaryResponse;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.SubscriptionTier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
public class StatsController {

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
    private final UserService users;

    public StatsController(AnalyticsClient analyticsClient, UserService users) {
        this.analyticsClient = analyticsClient;
        this.users = users;
    }

    @GetMapping("/stats/weekly")
    public String weekly(@AuthenticationPrincipal UserDetails me,
                         Model model) {
        UUID userId = users.findByUsernameOrThrow(me.getUsername()).getId();
        User u = users.findByUsernameOrThrow(me.getUsername());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);

        try {
            analyticsClient.recomputeWeeklyStats(userId, new RecomputeWeeklyRequest(start, end));
        } catch (Exception e) {
            System.err.println("Failed to refresh weekly stats in analytics microservice: " + e.getMessage());
        }

        WeeklySummaryResponse summary = null;
        try {
            var response = analyticsClient.getWeeklyStats(userId, start, end);
            summary = response != null ? response.getBody() : null;
        } catch (Exception e) {
            System.err.println("Error calling analytics microservice: " + e.getMessage());
        }

        if (summary == null) {
            summary = new WeeklySummaryResponse(start, end, java.util.Collections.emptyList());
        }

        model.addAttribute("summary", summary);
        model.addAttribute("from", start);
        model.addAttribute("to", end);

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
        model.addAttribute("isPro", u.getSubscriptionTier() == SubscriptionTier.PRO && u.isSubscriptionActive());
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
        User u = users.findByUsernameOrThrow(me.getUsername());
        if (u.getSubscriptionTier() != SubscriptionTier.PRO || !u.isSubscriptionActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Advanced Analytics is only available for PRO subscribers. Please upgrade your subscription to access this feature.");
            return "redirect:/subscription";
        }
        UUID userId = u.getId();

        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(90);

        TrainingFrequencyResponse trainingFrequency = null;
        List<ExerciseVolumeTrendDto> volumeTrends = null;
        List<ProgressiveOverloadDto> progressiveOverload = null;
        PersonalRecordsDto personalRecords = null;
        List<MilestoneDto> milestoneDtos = null;
        boolean microserviceAvailable = true;
        
        try {
            var freqResponse = analyticsClient.getTrainingFrequency(userId, start, end);
            trainingFrequency = freqResponse != null ? freqResponse.getBody() : null;
            
            var trendsResponse = analyticsClient.getExerciseVolumeTrends(userId, start, end);
            volumeTrends = trendsResponse != null ? trendsResponse.getBody() : null;
            
            var overloadResponse = analyticsClient.getProgressiveOverload(userId, start, end);
            progressiveOverload = overloadResponse != null ? overloadResponse.getBody() : null;
            
            var prResponse = analyticsClient.getPersonalRecords(userId);
            personalRecords = prResponse != null ? prResponse.getBody() : null;

            var milestonesResponse = analyticsClient.getMilestones(userId);
            milestoneDtos = milestonesResponse != null ? milestonesResponse.getBody() : null;
        } catch (feign.FeignException e) {
            microserviceAvailable = false;
            System.err.println("Error calling analytics microservice (FeignException): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            microserviceAvailable = false;
            System.err.println("Error calling analytics microservice: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (trainingFrequency == null) {
            trainingFrequency = new TrainingFrequencyResponse(
                    0, 
                    0.0, 
                    java.util.Collections.emptyMap(), 
                    java.util.Collections.emptyList(), 
                    0, 
                    0.0
            );
        }
        if (volumeTrends == null) {
            volumeTrends = java.util.Collections.emptyList();
        }
        if (progressiveOverload == null) {
            progressiveOverload = java.util.Collections.emptyList();
        }
        if (personalRecords == null) {
            personalRecords = new PersonalRecordsDto(java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }
        if (milestoneDtos == null) {
            milestoneDtos = java.util.Collections.emptyList();
        }

        List<MilestoneDto> manageableMilestones = milestoneDtos.stream()
                .filter(m -> !m.systemGenerated() && !AUTO_MILESTONE_TITLES.contains(m.title()))
                .toList();
        boolean hasSystemMilestones = milestoneDtos.stream()
                .anyMatch(m -> m.systemGenerated() || AUTO_MILESTONE_TITLES.contains(m.title()));

        model.addAttribute("navAvatar", u.getProfilePicture());
        model.addAttribute("username", u.getUsername());
        model.addAttribute("isAdmin", u.getRole() == UserRole.ADMIN);
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
        User u = users.findByUsernameOrThrow(me.getUsername());
        if (u.getSubscriptionTier() != SubscriptionTier.PRO || !u.isSubscriptionActive()) {
            redirectAttributes.addFlashAttribute("analyticsError", "You need an active PRO subscription to manage milestones.");
            return "redirect:/stats/advanced";
        }
        UUID userId = u.getId();

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
        User u = users.findByUsernameOrThrow(me.getUsername());
        if (u.getSubscriptionTier() != SubscriptionTier.PRO || !u.isSubscriptionActive()) {
            redirectAttributes.addFlashAttribute("analyticsError", "You need an active PRO subscription to manage milestones.");
            return "redirect:/stats/advanced";
        }
        UUID userId = u.getId();
        try {
            analyticsClient.deleteMilestone(userId, id);
            redirectAttributes.addFlashAttribute("analyticsMessage", "Milestone removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("analyticsError", "Failed to remove milestone: " + e.getMessage());
        }
        return "redirect:/stats/advanced";
    }
}
