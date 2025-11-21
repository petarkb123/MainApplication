package project.fitnessapplicationexam.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.*;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.service.UserService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
public class StatsApiController {

    private final AnalyticsClient analyticsClient;
    private final UserService users;

    public StatsApiController(AnalyticsClient analyticsClient, UserService users) {
        this.analyticsClient = analyticsClient;
        this.users = users;
    }

    @GetMapping("/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedStats(
            @AuthenticationPrincipal UserDetails me,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User u = users.findByUsernameOrThrow(me.getUsername());
        if (u.getSubscriptionTier() != SubscriptionTier.PRO || !u.isSubscriptionActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID userId = u.getId();
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(90);

        Map<String, Object> response = new HashMap<>();
        boolean microserviceAvailable = true;

        try {
            var freqResponse = analyticsClient.getTrainingFrequency(userId, start, end);
            TrainingFrequencyResponse trainingFrequency = freqResponse != null ? freqResponse.getBody() : null;
            response.put("trainingFrequency", trainingFrequency != null ? trainingFrequency : createEmptyTrainingFrequency());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("trainingFrequency", createEmptyTrainingFrequency());
            System.err.println("Error fetching training frequency: " + e.getMessage());
        }

        try {
            var trendsResponse = analyticsClient.getExerciseVolumeTrends(userId, start, end);
            List<ExerciseVolumeTrendDto> volumeTrends = trendsResponse != null ? trendsResponse.getBody() : null;
            response.put("volumeTrends", volumeTrends != null ? volumeTrends : List.of());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("volumeTrends", List.of());
            System.err.println("Error fetching volume trends: " + e.getMessage());
        }

        try {
            var overloadResponse = analyticsClient.getProgressiveOverload(userId, start, end);
            List<ProgressiveOverloadDto> progressiveOverload = overloadResponse != null ? overloadResponse.getBody() : null;
            response.put("progressiveOverload", progressiveOverload != null ? progressiveOverload : List.of());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("progressiveOverload", List.of());
            System.err.println("Error fetching progressive overload: " + e.getMessage());
        }

        try {
            var prResponse = analyticsClient.getPersonalRecords(userId);
            PersonalRecordsDto personalRecords = prResponse != null ? prResponse.getBody() : null;
            response.put("personalRecords", personalRecords != null ? personalRecords : new PersonalRecordsDto(List.of(), List.of()));
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("personalRecords", new PersonalRecordsDto(List.of(), List.of()));
            System.err.println("Error fetching personal records: " + e.getMessage());
        }

        try {
            var milestonesResponse = analyticsClient.getMilestones(userId);
            List<MilestoneDto> milestoneDtos = milestonesResponse != null ? milestonesResponse.getBody() : null;
            response.put("milestones", milestoneDtos != null ? milestoneDtos : List.of());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("milestones", List.of());
            System.err.println("Error fetching milestones: " + e.getMessage());
        }

        response.put("microserviceAvailable", microserviceAvailable);
        response.put("from", start);
        response.put("to", end);

        return ResponseEntity.ok(response);
    }

    private TrainingFrequencyResponse createEmptyTrainingFrequency() {
        return new TrainingFrequencyResponse(
                0,
                0.0,
                Map.of(),
                List.of(),
                0,
                0.0
        );
    }
}

