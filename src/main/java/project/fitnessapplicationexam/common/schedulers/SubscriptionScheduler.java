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

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);
    private final UserRepository users;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processRenewals() {
        LocalDate today = LocalDate.now();
        users.findAll().stream()
                .filter(User::isSubscriptionActive)
                .filter(u -> u.getSubscriptionTier() == SubscriptionTier.PRO)
                .filter(u -> u.getNextRenewalAt() != null && u.getNextRenewalAt().toLocalDate().isEqual(today))
                .forEach(u -> {
                    LocalDateTime newRenewal = u.getNextRenewalAt().plusDays(30);
                    u.setNextRenewalAt(newRenewal);
                    users.save(u);
                    log.info("Pro subscription renewed for user {} - next {}", u.getId(), newRenewal);
                });
    }

    @Scheduled(fixedRate = 1209600000)
    @Transactional
    public void notifyBasicUsersToUpgrade() {
        users.findAll().stream()
                .filter(u -> u.getSubscriptionTier() == SubscriptionTier.BASIC)
                .forEach(u -> {
                    log.info("BASIC user {} should consider upgrading to PRO subscription", u.getId());
                });
    }
}


