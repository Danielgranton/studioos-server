package com.studioos.server.beatmarketplace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatLikeResponse;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatLikeService {

    private final BeatRepository beatRepository;
    private final BeatLikeRepository beatLikeRepository;

    @Transactional(readOnly = true)
    public BeatLikeResponse getState(String beatId, User user) {
        Beat beat = findBeat(beatId);
        boolean liked = user != null && beatLikeRepository.existsByUserIdAndBeatId(user.getId(), beatId);
        return response(beat, liked);
    }

    @Transactional
    public BeatLikeResponse like(String beatId, User user) {
        requireUser(user);
        Beat beat = findBeat(beatId);
        if (!beatLikeRepository.existsByUserIdAndBeatId(user.getId(), beatId)) {
            beatLikeRepository.save(BeatLike.builder()
                    .userId(user.getId())
                    .beatId(beatId)
                    .build());
        }
        return response(beat, true);
    }

    @Transactional
    public BeatLikeResponse unlike(String beatId, User user) {
        requireUser(user);
        Beat beat = findBeat(beatId);
        beatLikeRepository.deleteByUserIdAndBeatId(user.getId(), beatId);
        return response(beat, false);
    }

    private Beat findBeat(String beatId) {
        return beatRepository.findById(beatId)
                .orElseThrow(() -> StudioosException.notFound("Beat not found"));
    }

    private BeatLikeResponse response(Beat beat, boolean liked) {
        long count = beatLikeRepository.countByBeatId(beat.getId());
        beat.setLikeCount(Math.toIntExact(count));
        return BeatLikeResponse.builder()
                .liked(liked)
                .likeCount(count)
                .build();
    }

    private void requireUser(User user) {
        if (user == null) {
            throw StudioosException.unauthorized("Authentication required to like a beat");
        }
    }
}
