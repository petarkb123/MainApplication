package project.fitnessapplicationexam.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.repository.UserRepository;
import project.fitnessapplicationexam.user.service.UserSubscriptionService;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSubscriptionService userSubscriptionService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .subscriptionTier(SubscriptionTier.BASIC)
                .subscriptionActive(true)
                .build();
    }

    @Test
    void getTier_returnsUserTier() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SubscriptionTier result = userSubscriptionService.getTier(userId);
        assertEquals(SubscriptionTier.BASIC, result);
    }

    @Test
    void getTier_userNotFound_returnsBasic() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        SubscriptionTier result = userSubscriptionService.getTier(userId);
        assertEquals(SubscriptionTier.BASIC, result);
    }

    @Test
    void activateBasic_setsBasicTier() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userSubscriptionService.activateBasic(userId);

        verify(userRepository).save(user);
        assertEquals(SubscriptionTier.BASIC, user.getSubscriptionTier());
        assertTrue(user.isSubscriptionActive());
        assertNull(user.getNextRenewalAt());
    }

    @Test
    void activatePro_setsProTier() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userSubscriptionService.activatePro(userId);

        verify(userRepository).save(user);
        assertEquals(SubscriptionTier.PRO, user.getSubscriptionTier());
        assertTrue(user.isSubscriptionActive());
        assertNotNull(user.getNextRenewalAt());
    }

    @Test
    void deactivateSubscription_setsSubscriptionInactive() {
        user.setSubscriptionActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userSubscriptionService.deactivateSubscription(userId);

        verify(userRepository).save(user);
        assertFalse(user.isSubscriptionActive());
    }

    @Test
    void isPro_proUser_returnsTrue() {
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userSubscriptionService.isPro(userId);
        assertTrue(result);
    }

    @Test
    void isPro_basicUser_returnsFalse() {
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userSubscriptionService.isPro(userId);
        assertFalse(result);
    }

    @Test
    void isPro_inactiveUser_returnsFalse() {
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userSubscriptionService.isPro(userId);
        assertFalse(result);
    }

    @Test
    void isPro_userNotFound_returnsFalse() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        boolean result = userSubscriptionService.isPro(userId);
        assertFalse(result);
    }
}
