package com.studioos.server.verification;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.VerificationStatus;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;
import com.studioos.server.verification.dto.VerificationCandidateResponse;
import com.studioos.server.verification.dto.VerificationReviewRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationReviewService {

    private final UserRepository userRepository;
    private final StudioRepository studioRepository;

    @Transactional
    public void requestUserVerification(User user) {
        requireCreator(user);
        if (!user.isAccountVerified()) {
            throw StudioosException.badRequest("Verify your account before requesting a blue tick");
        }
        user.setVerificationStatus(VerificationStatus.PENDING_REVIEW);
        userRepository.save(user);
    }

    @Transactional
    public void requestStudioVerification(User user, String studioId) {
        Studio studio = findOwnedStudio(user, studioId);
        studio.setVerificationStatus(VerificationStatus.PENDING_REVIEW);
        studioRepository.save(studio);
    }

    @Transactional(readOnly = true)
    public List<VerificationCandidateResponse> pendingUsers() {
        return userRepository.findByVerificationStatus(VerificationStatus.PENDING_REVIEW).stream()
                .map(user -> VerificationCandidateResponse.builder()
                        .id(String.valueOf(user.getId()))
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .verificationStatus(user.getVerificationStatus())
                        .reason(user.getVerificationReason())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificationCandidateResponse> pendingStudios() {
        return studioRepository.findByVerificationStatus(VerificationStatus.PENDING_REVIEW).stream()
                .map(studio -> VerificationCandidateResponse.builder()
                        .id(studio.getId())
                        .name(studio.getStudioName())
                        .verificationStatus(studio.getVerificationStatus())
                        .reason(studio.getVerificationReason())
                        .build())
                .toList();
    }

    @Transactional
    public VerificationStatus reviewUser(User reviewer, Integer userId, VerificationReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        validateDecision(request);
        user.setVerificationStatus(request.getStatus());
        user.setVerificationReason(trimToNull(request.getReason()));
        user.setVerificationReviewedAt(LocalDateTime.now());
        user.setVerificationReviewedBy(reviewer.getId());
        userRepository.save(user);
        return user.getVerificationStatus();
    }

    @Transactional
    public VerificationStatus reviewStudio(User reviewer, String studioId, VerificationReviewRequest request) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));
        validateDecision(request);
        studio.setVerificationStatus(request.getStatus());
        studio.setVerified(request.getStatus() == VerificationStatus.VERIFIED);
        studio.setVerificationReason(trimToNull(request.getReason()));
        studio.setVerificationReviewedAt(LocalDateTime.now());
        studio.setVerificationReviewedBy(reviewer.getId());
        studioRepository.save(studio);
        return studio.getVerificationStatus();
    }

    private void validateDecision(VerificationReviewRequest request) {
        if (request.getStatus() != VerificationStatus.VERIFIED
                && request.getStatus() != VerificationStatus.REJECTED) {
            throw StudioosException.badRequest("Review status must be VERIFIED or REJECTED");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireCreator(User user) {
        if (user == null || (user.getRole() != Role.ARTIST && user.getRole() != Role.PRODUCER)) {
            throw StudioosException.forbidden("Only artists and producers can request verification");
        }
    }

    private Studio findOwnedStudio(User user, String studioId) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));
        if (user == null || (!studio.getOwnerId().equals(user.getId()) && user.getRole() != Role.SUPER_ADMIN)) {
            throw StudioosException.forbidden("You do not own this studio");
        }
        return studio;
    }
}
