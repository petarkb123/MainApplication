package project.fitnessapplicationexam.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import project.fitnessapplicationexam.analytics.client.AnalyticsClient;
import project.fitnessapplicationexam.analytics.dto.PersonalRecordsDto;
import project.fitnessapplicationexam.analytics.dto.TrainingFrequencyResponse;
import project.fitnessapplicationexam.analytics.dto.WeeklySummaryResponse;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.model.UserRole;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private AnalyticsClient analyticsClient;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @Test
    void weeklyStats_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        WeeklySummaryResponse response = new WeeklySummaryResponse(LocalDate.now(), LocalDate.now(), List.of());
        when(analyticsClient.getWeeklyStats(any(), any(), any())).thenReturn(
                ResponseEntity.ok(response)
        );

        mockMvc.perform(get("/stats/weekly"))
                .andExpect(status().isOk())
                .andExpect(view().name("stats-weekly"));
    }

    @Test
    void advancedStats_withProUser_returnsPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(true);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);
        when(analyticsClient.getTrainingFrequency(any(), any(), any())).thenReturn(
                ResponseEntity.ok(new TrainingFrequencyResponse(0, 0.0, null, List.of(), 0, 0.0))
        );
        when(analyticsClient.getExerciseVolumeTrends(any(), any(), any())).thenReturn(ResponseEntity.ok(List.of()));
        when(analyticsClient.getProgressiveOverload(any(), any(), any())).thenReturn(ResponseEntity.ok(List.of()));
        when(analyticsClient.getPersonalRecords(any())).thenReturn(
                ResponseEntity.ok(new PersonalRecordsDto(List.of(), List.of()))
        );

        mockMvc.perform(get("/stats/advanced"))
                .andExpect(status().isOk())
                .andExpect(view().name("advanced-stats"));
    }

    @Test
    void advancedStats_withoutProUser_redirectsToSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(false);

        when(userService.findByUsernameOrThrow(anyString())).thenReturn(user);

        mockMvc.perform(get("/stats/advanced"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription"));
    }
}

