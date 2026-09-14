package com.studioos.server.reviews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.artist.ArtistReviewRepository;
import com.studioos.server.artist.ArtistReview;
import com.studioos.server.reviews.dto.ReviewCommentRequest;
import com.studioos.server.reviews.dto.ReviewInteractionResponse;
import com.studioos.server.reviews.dto.ReviewReactionRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.studio.StudioRatingRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReviewInteractionServiceTest {

    @Mock
    private ArtistReviewRepository artistReviewRepository;

    @Mock
    private ProducerReviewRepository producerReviewRepository;

    @Mock
    private StudioRatingRepository studioRatingRepository;

    @Mock
    private ReviewReactionRepository reactionRepository;

    @Mock
    private ReviewCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRateLimitService rateLimitService;

    @InjectMocks
    private ReviewInteractionService service;

    @Test
    void repeatedReactionTogglesOff() {
        User user = user(7, Role.ARTIST);
        ReviewReaction existing = ReviewReaction.builder()
                .reviewType(ReviewType.ARTIST)
                .reviewId("review-1")
                .userId(7)
                .reaction(ReviewReactionType.LIKE)
                .build();
        ReviewReactionRequest request = new ReviewReactionRequest();
        request.setReaction(ReviewReactionType.LIKE);

        when(artistReviewRepository.findByIdAndModerationStatus("review-1", ReviewModerationStatus.ACTIVE))
                .thenReturn(Optional.of(ArtistReview.builder().id("review-1").build()));
        when(reactionRepository.findByReviewTypeAndReviewIdAndUserId(ReviewType.ARTIST, "review-1", 7))
                .thenReturn(Optional.of(existing), Optional.empty());
        when(commentRepository.countByReviewTypeAndReviewIdAndDeletedAtIsNull(ReviewType.ARTIST, "review-1"))
                .thenReturn(0L);

        ReviewInteractionResponse result = service.react(user, ReviewType.ARTIST, "review-1", request);

        verify(reactionRepository).delete(existing);
        assertThat(result.getCurrentReaction()).isNull();
    }

    @Test
    void cannotReactToUnknownReview() {
        User user = user(7, Role.ARTIST);
        ReviewReactionRequest request = new ReviewReactionRequest();
        request.setReaction(ReviewReactionType.LIKE);
        when(artistReviewRepository.findByIdAndModerationStatus("missing", ReviewModerationStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.react(user, ReviewType.ARTIST, "missing", request))
                .isInstanceOf(StudioosException.class)
                .hasMessage("Review not found");
        verify(reactionRepository, never()).save(any(ReviewReaction.class));
    }

    @Test
    void userCannotDeleteAnotherUsersComment() {
        User owner = user(7, Role.USER);
        User otherUser = user(8, Role.USER);
        ReviewComment comment = ReviewComment.builder().id("comment-1").userId(8).user(otherUser).body("hello").build();
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.deleteComment(owner, "comment-1"))
                .isInstanceOf(StudioosException.class)
                .hasMessage("You can only delete your own review comments");
        verify(commentRepository, never()).save(any(ReviewComment.class));
    }

    @Test
    void moderatorCanSoftDeleteComment() {
        User moderator = user(2, Role.ADMIN);
        ReviewComment comment = ReviewComment.builder().id("comment-1").userId(8).body("hello").build();
        when(commentRepository.findById("comment-1")).thenReturn(Optional.of(comment));

        service.deleteComment(moderator, "comment-1");

        verify(commentRepository).save(comment);
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    private User user(Integer id, Role role) {
        return User.builder().id(id).name("Test user").email("test@example.com").role(role).build();
    }
}
