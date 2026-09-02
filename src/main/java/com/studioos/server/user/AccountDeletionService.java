package com.studioos.server.user;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.service.PasswordService;
import com.studioos.server.auth.service.SessionService;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.audit.AccountAuditService;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.user.dto.DeleteAccountRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final SessionService sessionService;
    private final AccountAuditService accountAuditService;

    @Transactional
    public void delete(User currentUser, DeleteAccountRequest request) {
        if (currentUser == null) throw StudioosException.unauthorized("Authentication required");
        if (request == null || !"DELETE".equals(request.getConfirmation())) {
            throw StudioosException.badRequest("Type DELETE to confirm account deletion");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        if (user.getDeletedAt() != null) {
            throw StudioosException.badRequest("Account has already been deleted");
        }
        if (passwordService.hasPassword(user)
                && !passwordService.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw StudioosException.badRequest("Current password is invalid");
        }

        sessionService.logoutAllDevices(user);
        accountAuditService.record(AuditEventType.ACCOUNT_DELETED, user, "Account deleted and personal data anonymized");
        user.setEmail("deleted+" + user.getId() + "@deleted.studioos.invalid");
        user.setPhone(null);
        user.setUsername(null);
        user.setName("Deleted user");
        user.setPasswordHash(null);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setAccountVerified(false);
        user.setProfileImage(null);
        user.setProfileImageLarge(null);
        user.setProfileImageMedium(null);
        user.setProfileImageThumbnail(null);
        user.setBio(null);
        user.setLocation(null);
        user.setGenre(null);
        user.setExperience(null);
        user.setInstagram(null);
        user.setYoutube(null);
        user.setLink(null);
        user.setSettings(null);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
