package project.fitnessapplicationexam.common.schedulers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.fitnessapplicationexam.user.model.SubscriptionTier;
import project.fitnessapplicationexam.user.model.User;
import project.fitnessapplicationexam.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import project.fitnessapplicationexam.config.ValidationConstants;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processRenewals() {
        LocalDate today = LocalDate.now();
        userRepository.findAll().stream()
                .filter(User::isSubscriptionActive)
                .filter(user -> user.getSubscriptionTier() == SubscriptionTier.PRO)
                .filter(user -> user.getNextRenewalAt() != null && user.getNextRenewalAt().toLocalDate().isEqual(today))
                .forEach(user -> {
                    LocalDateTime newRenewal = user.getNextRenewalAt().plusDays(ValidationConstants.SUBSCRIPTION_RENEWAL_DAYS);
                    user.setNextRenewalAt(newRenewal);
                    userRepository.save(user);
                    log.info("Pro subscription renewed for user {} - next {}", user.getId(), newRenewal);
                });
    }

    @Scheduled(fixedRate = 1209600000)
    @Transactional
    public void notifyBasicUsersToUpgrade() {
        userRepository.findAll().stream()
                .filter(user -> user.getSubscriptionTier() == SubscriptionTier.BASIC)
                .forEach(user -> {
                    log.info("BASIC user {} should consider upgrading to PRO subscription", user.getId());
                });
    }
}

