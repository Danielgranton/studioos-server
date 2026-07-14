package com.studioos.server.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.media.ResponsiveImageAsset;
import com.studioos.server.shared.media.ResponsiveImageProcessingService;
import com.studioos.server.user.dto.PublicUserResponse;
import com.studioos.server.user.dto.UpdateProfileRequest;
import com.studioos.server.user.dto.UserProfileResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ResponsiveImageProcessingService responsiveImageProcessingService;

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
        log.info("Profile updated for user: {}", user.getEmail());
        return toProfileResponse(user);
    }

    // ─── Get any user's public profile ───
    public PublicUserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        return toPublicResponse(user);
    }

    // ─── Mappers ───
    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .bio(user.getBio())
                .location(user.getLocation())
                .genre(user.getGenre())
                .experience(user.getExperience())
                .profileImage(user.getProfileImage())
                .profileImageLarge(user.getProfileImageLarge())
                .profileImageMedium(user.getProfileImageMedium())
                .profileImageThumbnail(user.getProfileImageThumbnail())
                .instagram(user.getInstagram())
                .youtube(user.getYoutube())
                .link(user.getLink())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PublicUserResponse toPublicResponse(User user) {
        return PublicUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .bio(user.getBio())
                .location(user.getLocation())
                .genre(user.getGenre())
                .experience(user.getExperience())
                .profileImage(user.getProfileImage())
                .profileImageLarge(user.getProfileImageLarge())
                .profileImageMedium(user.getProfileImageMedium())
                .profileImageThumbnail(user.getProfileImageThumbnail())
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

        user.setProfileImage(image.getOriginalUrl());
        user.setProfileImageLarge(image.getLargeUrl());
        user.setProfileImageMedium(image.getMediumUrl());
        user.setProfileImageThumbnail(image.getThumbnailUrl());
    }
}
