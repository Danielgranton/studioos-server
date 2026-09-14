package com.studioos.server.reviews;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.artist.ArtistReviewRepository;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.reviews.dto.ReviewCommentRequest;
import com.studioos.server.reviews.dto.ReviewCommentResponse;
import com.studioos.server.reviews.dto.ReviewInteractionResponse;
import com.studioos.server.reviews.dto.ReviewReactionRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.studio.StudioRatingRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewInteractionService {

    private final ArtistReviewRepository artistReviewRepository;
    private final ProducerReviewRepository producerReviewRepository;
    private final StudioRatingRepository studioRatingRepository;
    private final ReviewReactionRepository reactionRepository;
    private final ReviewCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ReviewRateLimitService rateLimitService;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public ReviewInteractionResponse react(User user, ReviewType type, String reviewId, ReviewReactionRequest request) {
        rateLimitService.checkReaction(user);
        ensureReviewExists(type, reviewId);
        ReviewReaction reaction = reactionRepository
                .findByReviewTypeAndReviewIdAndUserId(type, reviewId, user.getId())
                .orElse(null);

        if (reaction != null && reaction.getReaction() == request.getReaction()) {
            reactionRepository.delete(reaction);
        } else if (reaction != null) {
            reaction.setReaction(request.getReaction());
            reactionRepository.save(reaction);
            notifyReviewAuthor(reviewAuthorId(type, reviewId), user, NotificationType.REVIEW_REACTION,
                    "New reaction on your review",
                    actorName(user) + " changed their reaction on your review.", reviewId);
        } else {
            reactionRepository.save(ReviewReaction.builder()
                    .reviewType(type)
                    .reviewId(reviewId)
                    .userId(user.getId())
                    .reaction(request.getReaction())
                    .build());
            notifyReviewAuthor(reviewAuthorId(type, reviewId), user, NotificationType.REVIEW_REACTION,
                    "New reaction on your review",
                    actorName(user) + " reacted to your review.", reviewId);
        }
        return summary(user, type, reviewId);
    }

    @Transactional
    public ReviewInteractionResponse clearReaction(User user, ReviewType type, String reviewId) {
        rateLimitService.checkReaction(user);
        ensureReviewExists(type, reviewId);
        reactionRepository.findByReviewTypeAndReviewIdAndUserId(type, reviewId, user.getId())
                .ifPresent(reactionRepository::delete);
        return summary(user, type, reviewId);
    }

    @Transactional(readOnly = true)
    public ReviewInteractionResponse summary(User user, ReviewType type, String reviewId) {
        ensureReviewExists(type, reviewId);
        ReviewReactionType current = user == null ? null : reactionRepository
                .findByReviewTypeAndReviewIdAndUserId(type, reviewId, user.getId())
                .map(ReviewReaction::getReaction)
                .orElse(null);
        return ReviewInteractionResponse.builder()
                .reviewType(type)
                .reviewId(reviewId)
                .currentReaction(current)
                .likes(reactionRepository.countByReviewTypeAndReviewIdAndReaction(type, reviewId, ReviewReactionType.LIKE))
                .dislikes(reactionRepository.countByReviewTypeAndReviewIdAndReaction(type, reviewId, ReviewReactionType.DISLIKE))
                .comments(commentRepository.countByReviewTypeAndReviewIdAndDeletedAtIsNull(type, reviewId))
                .build();
    }

    @Transactional
    public ReviewCommentResponse addComment(User user, ReviewType type, String reviewId, ReviewCommentRequest request) {
        rateLimitService.checkComment(user);
        ensureReviewExists(type, reviewId);
        ReviewComment comment = commentRepository.save(ReviewComment.builder()
                .reviewType(type)
                .reviewId(reviewId)
                .userId(user.getId())
                .body(request.getBody().trim())
                .build());
        comment.setUser(user);
        notifyReviewAuthor(reviewAuthorId(type, reviewId), user, NotificationType.REVIEW_COMMENT,
                "New comment on your review",
                actorName(user) + " commented on your review.", reviewId);
        return toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<ReviewCommentResponse> getComments(ReviewType type, String reviewId, Pageable pageable) {
        ensureReviewExists(type, reviewId);
        return commentRepository.findByReviewTypeAndReviewIdAndDeletedAtIsNull(type, reviewId, pageable)
                .map(this::toCommentResponse);
    }

    @Transactional
    public void deleteComment(User user, String commentId) {
        ReviewComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> StudioosException.notFound("Review comment not found"));
        boolean moderator = user.getRole().name().equals("ADMIN") || user.getRole().name().equals("SUPER_ADMIN");
        if (!moderator && !comment.getUserId().equals(user.getId())) {
            throw StudioosException.forbidden("You can only delete your own review comments");
        }
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    private void ensureReviewExists(ReviewType type, String reviewId) {
        boolean exists = switch (type) {
            case ARTIST -> artistReviewRepository.findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE).isPresent();
            case PRODUCER -> producerReviewRepository.findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE).isPresent();
            case STUDIO -> studioRatingRepository.findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE).isPresent();
        };
        if (!exists) throw StudioosException.notFound("Review not found");
    }

    private Integer reviewAuthorId(ReviewType type, String reviewId) {
        return switch (type) {
            case ARTIST -> artistReviewRepository
                    .findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE)
                    .map(com.studioos.server.artist.ArtistReview::getReviewerId)
                    .orElseThrow(() -> StudioosException.notFound("Review not found"));
            case PRODUCER -> producerReviewRepository
                    .findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE)
                    .map(ProducerReview::getUserId)
                    .orElseThrow(() -> StudioosException.notFound("Review not found"));
            case STUDIO -> studioRatingRepository
                    .findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE)
                    .map(com.studioos.server.studio.StudioRating::getUserId)
                    .orElseThrow(() -> StudioosException.notFound("Review not found"));
        };
    }

    private void notifyReviewAuthor(Integer reviewAuthorId, User actor, NotificationType type,
                                    String title, String message, String reviewId) {
        if (reviewAuthorId == null || reviewAuthorId.equals(actor.getId())) {
            return;
        }

        try {
            CreateNotificationRequest request = new CreateNotificationRequest();
            request.setUserId(reviewAuthorId);
            request.setType(type);
            request.setTitle(title);
            request.setMessage(message);
            request.setRelatedEntityId(reviewId);
            notificationService.createNotification(request);
        } catch (RuntimeException exception) {
            log.warn("Could not create review notification for review {}", reviewId, exception);
        }
    }

    private String actorName(User actor) {
        if (actor.getName() != null && !actor.getName().isBlank()) {
            return actor.getName();
        }
        return actor.getUsername() != null ? actor.getUsername() : "Someone";
    }

    private ReviewCommentResponse toCommentResponse(ReviewComment comment) {
        User user = comment.getUser() != null ? comment.getUser() : userRepository.findById(comment.getUserId()).orElse(null);
        return ReviewCommentResponse.builder()
                .id(comment.getId())
                .reviewType(comment.getReviewType())
                .reviewId(comment.getReviewId())
                .userId(comment.getUserId())
                .userName(user != null ? user.getName() : null)
                .username(user != null ? user.getUsername() : null)
                .avatar(user != null ? user.getProfileImageThumbnail() : null)
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
