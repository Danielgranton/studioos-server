package com.studioos.server.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingAccountExpiryScheduler {

    private static final int PENDING_ACCOUNT_TTL_MINUTES = 30;

    private final UserRepository userRepository;

    @Scheduled(fixedDelayString = "${auth.pending-account-expiry-scan-ms:300000}")
    @Transactional
    public void expirePendingAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_ACCOUNT_TTL_MINUTES);
        List<User> pendingUsers = userRepository.findByStatusAndCreatedAtBefore(AccountStatus.PENDING, cutoff);
        if (pendingUsers.isEmpty()) return;
        pendingUsers.forEach(user -> user.setStatus(AccountStatus.EXPIRED));
        userRepository.saveAll(pendingUsers);
        log.info("Expired {} pending account(s)", pendingUsers.size());
    }
}
