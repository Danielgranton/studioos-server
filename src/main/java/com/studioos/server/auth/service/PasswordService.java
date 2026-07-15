package com.studioos.server.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public String hash(String rawPassword) {
        return rawPassword == null || rawPassword.isBlank() ? null : passwordEncoder.encode(rawPassword);
    }

    public boolean hasPassword(User user) {
        return user != null && user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && passwordHash != null && passwordEncoder.matches(rawPassword, passwordHash);
    }
}
