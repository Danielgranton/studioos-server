package com.studioos.server.engagement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.engagement.dto.EngagementRequest;
import com.studioos.server.engagement.dto.EngagementStateResponse;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private final EngagementEdgeRepository edgeRepository;
    private final EngagementViewRepository viewRepository;
    private final UserRepository userRepository;
    private final StudioRepository studioRepository;

    @Transactional
    public EngagementStateResponse follow(User actor, EngagementRequest request) {
        return setEdge(actor, request, EngagementAction.FOLLOW, true);
    }

    @Transactional
    public EngagementStateResponse unfollow(User actor, EngagementRequest request) {
        return setEdge(actor, request, EngagementAction.FOLLOW, false);
    }

    @Transactional
    public EngagementStateResponse favorite(User actor, EngagementRequest request) {
        return setEdge(actor, request, EngagementAction.FAVORITE, true);
    }

    @Transactional
    public EngagementStateResponse unfavorite(User actor, EngagementRequest request) {
        return setEdge(actor, request, EngagementAction.FAVORITE, false);
    }

    @Transactional
    public EngagementStateResponse recordView(User viewer, EngagementRequest request) {
        validateTarget(request);
        viewRepository.save(EngagementView.builder()
                .viewerId(viewer == null ? null : viewer.getId())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .build());
        return state(viewer, request);
    }

    private EngagementStateResponse setEdge(User actor, EngagementRequest request, EngagementAction action, boolean enabled) {
        if (actor == null) throw StudioosException.unauthorized("Authentication required");
        validateTarget(request);
        if (request.getTargetType() == EngagementTargetType.USER
                && request.getTargetId().equals(String.valueOf(actor.getId()))) {
            throw StudioosException.badRequest("You cannot engage with your own profile");
        }

        var existing = edgeRepository.findByActorIdAndTargetTypeAndTargetIdAndAction(
                actor.getId(), request.getTargetType(), request.getTargetId(), action);
        if (enabled && existing.isEmpty()) {
            edgeRepository.save(EngagementEdge.builder()
                    .actorId(actor.getId())
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .action(action)
                    .build());
        } else if (!enabled) {
            existing.ifPresent(edgeRepository::delete);
        }
        return state(actor, request);
    }

    private EngagementStateResponse state(User actor, EngagementRequest request) {
        boolean following = actor != null && edgeRepository
                .findByActorIdAndTargetTypeAndTargetIdAndAction(actor.getId(), request.getTargetType(), request.getTargetId(), EngagementAction.FOLLOW)
                .isPresent();
        boolean favorited = actor != null && edgeRepository
                .findByActorIdAndTargetTypeAndTargetIdAndAction(actor.getId(), request.getTargetType(), request.getTargetId(), EngagementAction.FAVORITE)
                .isPresent();
        return EngagementStateResponse.builder()
                .active(following || favorited)
                .followerCount(edgeRepository.countByTargetTypeAndTargetIdAndAction(request.getTargetType(), request.getTargetId(), EngagementAction.FOLLOW))
                .favoriteCount(edgeRepository.countByTargetTypeAndTargetIdAndAction(request.getTargetType(), request.getTargetId(), EngagementAction.FAVORITE))
                .viewCount(viewRepository.countByTargetTypeAndTargetId(request.getTargetType(), request.getTargetId()))
                .build();
    }

    private void validateTarget(EngagementRequest request) {
        if (request.getTargetType() == EngagementTargetType.USER) {
            try {
                if (!userRepository.existsById(Integer.valueOf(request.getTargetId()))) throw StudioosException.notFound("User not found");
            } catch (NumberFormatException exception) {
                throw StudioosException.badRequest("Invalid user target");
            }
        } else if (!studioRepository.existsById(request.getTargetId())) {
            throw StudioosException.notFound("Studio not found");
        }
    }
}
