package project.fitnessapplicationexam.common.schedulers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionScheduler subscriptionScheduler;

    @Test
    void processRenewals_renewsActiveProUsers() {
        LocalDateTime todayRenewal = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        User proUser = User.builder()
                .id(UUID.randomUUID())
                .subscriptionTier(SubscriptionTier.PRO)
                .subscriptionActive(true)
                .nextRenewalAt(todayRenewal)
                .build();

        User basicUser = User.builder()
                .id(UUID.randomUUID())
                .subscriptionTier(SubscriptionTier.BASIC)
                .subscriptionActive(true)
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(proUser, basicUser));
        when(userRepository.save(any(User.class))).thenReturn(proUser);

        subscriptionScheduler.processRenewals();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void notifyBasicUsersToUpgrade_logsBasicUsers() {
        User basicUser = User.builder()
                .id(UUID.randomUUID())
                .subscriptionTier(SubscriptionTier.BASIC)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(basicUser));

        subscriptionScheduler.notifyBasicUsersToUpgrade();

        verify(userRepository).findAll();
    }

    @Test
    void processRenewals_skipsInactiveUsers() {
        User inactiveProUser = User.builder()
                .id(UUID.randomUUID())
                .subscriptionTier(SubscriptionTier.PRO)
                .subscriptionActive(false)
                .nextRenewalAt(LocalDateTime.now().plusDays(1))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(inactiveProUser));

        subscriptionScheduler.processRenewals();

        verify(userRepository, never()).save(any(User.class));
    }
}

