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

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(UserSubscriptionService.class);
    private final UserRepository users;

    @Transactional(readOnly = true)
    public SubscriptionTier getTier(UUID userId) {
        return users.findById(userId).map(User::getSubscriptionTier)
                .orElse(SubscriptionTier.BASIC);
    }

    @Transactional
    public void activateBasic(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        u.setSubscriptionTier(SubscriptionTier.BASIC);
        u.setSubscriptionActive(true);
        u.setNextRenewalAt(null);
        users.save(u);
        log.info("User {} switched to BASIC", userId);
    }

    @Transactional
    public void activatePro(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        u.setSubscriptionTier(SubscriptionTier.PRO);
        u.setSubscriptionActive(true);
        u.setNextRenewalAt(LocalDateTime.now().plusDays(30));
        users.save(u);
        log.info("User {} switched to PRO; next renewal {}", userId, u.getNextRenewalAt());
    }

    @Transactional
    public void deactivateSubscription(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        u.setSubscriptionActive(false);
        users.save(u);
        log.info("User {} subscription deactivated", userId);
    }

    @Transactional(readOnly = true)
    public boolean isPro(UUID userId) {
        User u = users.findById(userId).orElse(null);
        return u != null && u.isSubscriptionActive() && u.getSubscriptionTier() == SubscriptionTier.PRO;
    }
}


