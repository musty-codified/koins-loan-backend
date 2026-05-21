package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.enums.AccountStatus;
import com.koins.loanbackend.domain.enums.UserRole;
import com.koins.loanbackend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seed() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("Admin user [{}] already exists — skipping seed", adminEmail);
            return;
        }

        User admin = new User();
        admin.setName("System Administrator");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setRole(UserRole.ADMIN);

        try {
            userRepository.save(admin);
            log.info("Admin user seeded successfully [{}]", adminEmail);
            warnIfDefaultCredentials();
        } catch (DataIntegrityViolationException e) {
            // Another instance seeded concurrently — safe to ignore
            log.debug("Admin user already exists (concurrent seed) — skipping");
        }
    }

    private void warnIfDefaultCredentials() {
        if ("admin@koins.com".equals(adminEmail)) {
            log.warn("======================================================");
            log.warn("  SECURITY WARNING: Default admin credentials in use.  ");
            log.warn("  Set ADMIN_EMAIL and ADMIN_PASSWORD env vars in prod. ");
            log.warn("======================================================");
        }
    }
}