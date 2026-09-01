package com.studioos.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.service.PasswordService;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

/** Creates the initial platform administrator only when explicitly requested. */
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Value("${bootstrap.super-admin.email:}")
    private String email;

    @Value("${bootstrap.super-admin.name:StudioOS Super Admin}")
    private String name;

    @Value("${bootstrap.super-admin.phone:}")
    private String phone;

    @Value("${bootstrap.super-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!args.containsOption("bootstrap-super-admin")) {
            return;
        }

        if (userRepository.findByRole(Role.SUPER_ADMIN).stream().findAny().isPresent()) {
            throw new IllegalStateException("A SUPER_ADMIN already exists; bootstrap refused");
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_SUPER_ADMIN_EMAIL and BOOTSTRAP_SUPER_ADMIN_PASSWORD are required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("A user with the bootstrap email already exists");
        }

        User admin = User.builder()
                .name(name)
                .email(email)
                .phone(phone.isBlank() ? null : phone)
                .passwordHash(passwordService.hash(password))
                .role(Role.SUPER_ADMIN)
                .emailVerified(true)
                .phoneVerified(phone.isBlank())
                .accountVerified(true)
                .build();

        userRepository.save(admin);
    }
}
