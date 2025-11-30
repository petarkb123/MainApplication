package project.fitnessapplicationexam.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import project.fitnessapplicationexam.config.ValidationConstants;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsApiController {

    private static final Logger log = LoggerFactory.getLogger(StatsApiController.class);
    private final AnalyticsClient analyticsClient;
    private final UserService userService;

    @GetMapping("/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedStats(
            @AuthenticationPrincipal UserDetails me,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findByUsernameOrThrow(me.getUsername());
        if (user.getSubscriptionTier() != SubscriptionTier.PRO || !user.isSubscriptionActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID userId = user.getId();
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(ValidationConstants.DEFAULT_ANALYTICS_DAYS);

        Map<String, Object> response = new HashMap<>();
        boolean microserviceAvailable = true;

        try {
            ResponseEntity<TrainingFrequencyResponse> trainingFrequencyResponse = analyticsClient.getTrainingFrequency(userId, start, end);
            response.put("trainingFrequency", trainingFrequencyResponse != null && trainingFrequencyResponse.getBody() != null 
                    ? trainingFrequencyResponse.getBody() 
                    : createEmptyTrainingFrequency());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("trainingFrequency", createEmptyTrainingFrequency());
            log.error("Error fetching training frequency for user {}", userId, e);
        }

        try {
            ResponseEntity<List<ExerciseVolumeTrendDto>> volumeTrendsResponse = analyticsClient.getExerciseVolumeTrends(userId, start, end);
            response.put("volumeTrends", volumeTrendsResponse != null && volumeTrendsResponse.getBody() != null 
                    ? volumeTrendsResponse.getBody() 
                    : List.of());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("volumeTrends", List.of());
            log.error("Error fetching volume trends for user {}", userId, e);
        }

        try {
            ResponseEntity<List<ProgressiveOverloadDto>> progressiveOverloadResponse = analyticsClient.getProgressiveOverload(userId, start, end);
            response.put("progressiveOverload", progressiveOverloadResponse != null && progressiveOverloadResponse.getBody() != null 
                    ? progressiveOverloadResponse.getBody() 
                    : List.of());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("progressiveOverload", List.of());
            log.error("Error fetching progressive overload for user {}", userId, e);
        }

        try {
            ResponseEntity<PersonalRecordsDto> personalRecordsResponse = analyticsClient.getPersonalRecords(userId);
            response.put("personalRecords", personalRecordsResponse != null && personalRecordsResponse.getBody() != null 
                    ? personalRecordsResponse.getBody() 
                    : new PersonalRecordsDto(List.of(), List.of()));
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("personalRecords", new PersonalRecordsDto(List.of(), List.of()));
            log.error("Error fetching personal records for user {}", userId, e);
        }

        try {
            ResponseEntity<List<MilestoneDto>> milestonesResponse = analyticsClient.getMilestones(userId);
            response.put("milestones", milestonesResponse != null && milestonesResponse.getBody() != null 
                    ? milestonesResponse.getBody() 
                    : List.of());
        } catch (Exception e) {
            microserviceAvailable = false;
            response.put("milestones", List.of());
            log.error("Error fetching milestones for user {}", userId, e);
        }

        response.put("microserviceAvailable", microserviceAvailable);
        response.put("from", start);
        response.put("to", end);

        return ResponseEntity.ok(response);
    }

    private TrainingFrequencyResponse createEmptyTrainingFrequency() {
        return new TrainingFrequencyResponse(0, 0.0, Map.of(), List.of(), 0, 0.0);
    }
}

