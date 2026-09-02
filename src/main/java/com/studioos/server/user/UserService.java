package com.studioos.server.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.audit.AccountAuditService;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.media.ResponsiveImageAsset;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.auth.service.ProfileImageServiceClient;
import com.studioos.server.shared.media.ResponsiveImageProcessingService;
import com.studioos.server.user.dto.PublicUserResponse;
import com.studioos.server.user.dto.UpdateProfileRequest;
import com.studioos.server.user.dto.UpdateUsernameRequest;
import com.studioos.server.user.dto.UserProfileResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ResponsiveImageProcessingService responsiveImageProcessingService;
    private final ProfileImageServiceClient profileImageServiceClient;
    private final PresignedUrlService presignedUrlService;
    private final AccountAuditService accountAuditService;
    private final PrivacySettingsService privacySettingsService;

    @Value("${storage.s3.profile-url-expiry-seconds:3600}")
    private int profileUrlExpirySeconds;

    // ─── Get own profile ───
    public UserProfileResponse getMyProfile(User currentUser) {
        return toProfileResponse(currentUser);
    }

    // ─── Update own profile ───
    @Transactional
    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getGenre() != null) user.setGenre(request.getGenre());
        if (request.getExperience() != null) user.setExperience(request.getExperience());
        if (request.getProfileImage() != null) applyProfileImage(user, request.getProfileImage());
        if (request.getInstagram() != null) user.setInstagram(request.getInstagram());
        if (request.getYoutube() != null) user.setYoutube(request.getYoutube());
        if (request.getLink() != null) user.setLink(request.getLink());

        userRepository.save(user);
        accountAuditService.record(AuditEventType.PROFILE_UPDATED, user, "Profile details updated");
        log.info("Profile updated for user: {}", user.getEmail());
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUsername(User currentUser, UpdateUsernameRequest request) {
        String username = request == null || request.getUsername() == null
                ? ""
                : request.getUsername().trim().toLowerCase();

        if (!username.matches("[a-z0-9_]{3,30}")) {
            throw StudioosException.badRequest(
                    "Username must be 3-30 characters and use only letters, numbers, or underscores");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        if (username.equals(user.getUsername())) {
            return toProfileResponse(user);
        }
        if (userRepository.existsByUsername(username)) {
            throw StudioosException.conflict("Username is already in use");
        }

        user.setUsername(username);
        userRepository.save(user);
        accountAuditService.record(AuditEventType.USERNAME_CHANGED, user, "Username changed");
        log.info("Username updated for user id: {}", user.getId());
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfileImage(User currentUser, MultipartFile file) {
        if (currentUser == null) throw StudioosException.unauthorized("Authentication required");
        if (file == null || file.isEmpty()) throw StudioosException.badRequest("Profile image is required");
        if (file.getSize() > 5L * 1024L * 1024L) {
            throw StudioosException.badRequest("Profile image must not exceed 5 MB");
        }
        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {
            throw StudioosException.badRequest("Only image files are supported");
        }

        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> StudioosException.notFound("User not found"));
            ResponsiveImageAsset image;
            try (var input = file.getInputStream()) {
                image = profileImageServiceClient.processUploadedProfileImage(
                        input, file.getSize(), file.getOriginalFilename(), contentType,
                        "users/" + user.getId() + "/profile", String.valueOf(user.getId()));
            }
            applyImage(user, image);
            userRepository.save(user);
            accountAuditService.record(AuditEventType.PROFILE_IMAGE_CHANGED, user, "Profile image changed");
            return toProfileResponse(user);
        } catch (java.io.IOException e) {
            throw StudioosException.badRequest("Could not read profile image");
        }
    }

    // ─── Get any user's public profile ───
    public PublicUserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        if (!privacySettingsService.isProfileDiscoverable(user.getId())) {
            throw StudioosException.notFound("User not found");
        }
        return toPublicResponse(user);
    }

    // ─── Mappers ───
    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .bio(user.getBio())
                .location(user.getLocation())
                .genre(user.getGenre())
                .experience(user.getExperience())
                .profileImage(resolveImageUrl(user.getProfileImage()))
                .profileImageLarge(resolveImageUrl(user.getProfileImageLarge()))
                .profileImageMedium(resolveImageUrl(user.getProfileImageMedium()))
                .profileImageThumbnail(resolveImageUrl(user.getProfileImageThumbnail()))
                .instagram(user.getInstagram())
                .youtube(user.getYoutube())
                .link(user.getLink())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PublicUserResponse toPublicResponse(User user) {
        PrivacySettings privacy = privacySettingsService.getEntityOrDefaults(user.getId());
        return PublicUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(privacy.isEmailVisible() ? user.getEmail() : null)
                .phone(privacy.isPhoneVisible() ? user.getPhone() : null)
                .role(user.getRole())
                .bio(user.getBio())
                .location(user.getLocation())
                .genre(user.getGenre())
                .experience(user.getExperience())
                .profileImage(resolveImageUrl(user.getProfileImage()))
                .profileImageLarge(resolveImageUrl(user.getProfileImageLarge()))
                .profileImageMedium(resolveImageUrl(user.getProfileImageMedium()))
                .profileImageThumbnail(resolveImageUrl(user.getProfileImageThumbnail()))
                .instagram(user.getInstagram())
                .youtube(user.getYoutube())
                .link(user.getLink())
                .build();
    }

    private void applyProfileImage(User user, String profileImageReference) {
        ResponsiveImageAsset image = responsiveImageProcessingService.process(
                profileImageReference,
                "users/" + user.getId() + "/profile");
        if (image == null) {
            return;
        }

        applyImage(user, image);
    }

    private void applyImage(User user, ResponsiveImageAsset image) {
        if (image == null) return;
        user.setProfileImage(image.getOriginalUrl());
        user.setProfileImageLarge(image.getLargeUrl());
        user.setProfileImageMedium(image.getMediumUrl());
        user.setProfileImageThumbnail(image.getThumbnailUrl());
    }

    private String resolveImageUrl(String reference) {
        if (reference == null || !reference.startsWith("s3://")) return reference;
        String remainder = reference.substring("s3://".length());
        int separator = remainder.indexOf('/');
        if (separator <= 0 || separator == remainder.length() - 1) return reference;
        return presignedUrlService.generateDownloadUrl(
                remainder.substring(0, separator),
                remainder.substring(separator + 1),
                profileUrlExpirySeconds);
    }
}
