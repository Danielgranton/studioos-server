package com.studioos.server.user;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.audit.AccountAuditService;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.user.dto.PrivacySettingsResponse;
import com.studioos.server.user.dto.UpdatePrivacySettingsRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrivacySettingsService {

    private final PrivacySettingsRepository repository;
    private final AccountAuditService accountAuditService;

    @Transactional(readOnly = true)
    public PrivacySettingsResponse get(User currentUser) {
        requireUser(currentUser);
        return toResponse(repository.findByUserId(currentUser.getId()).orElseGet(() -> defaults(currentUser.getId())));
    }

    @Transactional
    public PrivacySettingsResponse update(User currentUser, UpdatePrivacySettingsRequest request) {
        requireUser(currentUser);
        PrivacySettings settings = repository.findByUserId(currentUser.getId())
                .orElseGet(() -> defaults(currentUser.getId()));

        if (request == null) {
            throw StudioosException.badRequest("Privacy settings are required");
        }
        if (request.getProfileDiscoverable() != null) settings.setProfileDiscoverable(request.getProfileDiscoverable());
        if (request.getEmailVisible() != null) settings.setEmailVisible(request.getEmailVisible());
        if (request.getPhoneVisible() != null) settings.setPhoneVisible(request.getPhoneVisible());
        if (request.getDirectMessagesEnabled() != null) settings.setDirectMessagesEnabled(request.getDirectMessagesEnabled());
        if (request.getPersonalizedRecommendations() != null) settings.setPersonalizedRecommendations(request.getPersonalizedRecommendations());
        settings.setUpdatedAt(LocalDateTime.now());

        PrivacySettings saved = repository.save(settings);
        accountAuditService.record(AuditEventType.PRIVACY_UPDATED, currentUser, "Privacy settings updated");
        return toResponse(saved);
    }

    public boolean isProfileDiscoverable(Integer userId) {
        return repository.findByUserId(userId).map(PrivacySettings::isProfileDiscoverable).orElse(true);
    }

    public boolean isPersonalizedRecommendationsEnabled(Integer userId) {
        return repository.findByUserId(userId)
                .map(PrivacySettings::isPersonalizedRecommendations)
                .orElse(true);
    }

    public void requireDirectMessagesEnabled(Integer userId) {
        boolean enabled = repository.findByUserId(userId)
                .map(PrivacySettings::isDirectMessagesEnabled)
                .orElse(true);
        if (!enabled) {
            throw StudioosException.forbidden("This user does not accept direct messages");
        }
    }

    public PrivacySettings getEntityOrDefaults(Integer userId) {
        return repository.findByUserId(userId).orElseGet(() -> defaults(userId));
    }

    private PrivacySettings defaults(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        return PrivacySettings.builder().userId(userId).createdAt(now).updatedAt(now).build();
    }

    private PrivacySettingsResponse toResponse(PrivacySettings settings) {
        return PrivacySettingsResponse.builder()
                .profileDiscoverable(settings.isProfileDiscoverable())
                .emailVisible(settings.isEmailVisible())
                .phoneVisible(settings.isPhoneVisible())
                .directMessagesEnabled(settings.isDirectMessagesEnabled())
                .personalizedRecommendations(settings.isPersonalizedRecommendations())
                .build();
    }

    private void requireUser(User user) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
    }
}
