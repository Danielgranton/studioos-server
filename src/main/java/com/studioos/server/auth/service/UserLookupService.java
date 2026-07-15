package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    public User findByIdentifier(String identifier) {
        return userRepository.findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() -> StudioosException.notFound("No account found with this email or phone number"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> StudioosException.notFound("User not found"));
    }
}
