package project.fitnessapplicationexam.user.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import project.fitnessapplicationexam.config.ValidationConstants;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(UserSubscriptionService.class);
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SubscriptionTier getTier(UUID userId) {
        return userRepository.findById(userId).map(User::getSubscriptionTier)
                .orElse(SubscriptionTier.BASIC);
    }

    @Transactional
    public void activateBasic(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setSubscriptionTier(SubscriptionTier.BASIC);
        user.setSubscriptionActive(true);
        user.setNextRenewalAt(null);
        userRepository.save(user);
        log.info("User {} switched to BASIC", userId);
    }

    @Transactional
    public void activatePro(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionActive(true);
        user.setNextRenewalAt(LocalDateTime.now().plusDays(ValidationConstants.SUBSCRIPTION_RENEWAL_DAYS));
        userRepository.save(user);
        log.info("User {} switched to PRO; next renewal {}", userId, user.getNextRenewalAt());
    }

    @Transactional
    public void deactivateSubscription(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setSubscriptionActive(false);
        userRepository.save(user);
        log.info("User {} subscription deactivated", userId);
    }

    @Transactional(readOnly = true)
    public boolean isPro(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.isSubscriptionActive() && user.getSubscriptionTier() == SubscriptionTier.PRO;
    }
}


