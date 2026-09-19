package com.studioos.server.beatmarketplace;

import com.studioos.server.beatmarketplace.dto.BeatUploadCompleteResponse;
import com.studioos.server.beatmarketplace.dto.BeatUploadSessionResponse;
import com.studioos.server.beatmarketplace.dto.CreateBeatRequest;
import com.studioos.server.beatmarketplace.dto.UpdateBeatRequest;
import com.studioos.server.beatmarketplace.dto.RefreshUploadSessionResponse;
import com.studioos.server.beatmarketplace.dto.BeatSaleResponse;
import com.studioos.server.beatmarketplace.dto.BeatSummaryResponse;
import com.studioos.server.beatmarketplace.dto.BeatLikeResponse;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatController {

    private final BeatService beatService;
    private final BeatBrowseService beatBrowseService;
    private final BeatGenreRepository beatGenreRepository;
    private final BeatLikeService beatLikeService;

    @GetMapping("/{beatId}/like")
    public BeatLikeResponse getLikeState(
            @PathVariable String beatId,
            @AuthenticationPrincipal User user) {
        return beatLikeService.getState(beatId, user);
    }

    @PostMapping("/{beatId}/like")
    public BeatLikeResponse like(
            @PathVariable String beatId,
            @AuthenticationPrincipal User user) {
        return beatLikeService.like(beatId, user);
    }

    @DeleteMapping("/{beatId}/like")
    public BeatLikeResponse unlike(
            @PathVariable String beatId,
            @AuthenticationPrincipal User user) {
        return beatLikeService.unlike(beatId, user);
    }

    @GetMapping("/my")
    public java.util.List<BeatSummaryResponse> getMyBeats(@AuthenticationPrincipal User producer) {
        return beatBrowseService.getMyBeats(producer.getId());
    }

    @GetMapping("/my/sales")
    public java.util.List<BeatSaleResponse> getMySales(@AuthenticationPrincipal User producer) {
        return beatBrowseService.getMySales(producer.getId());
    }

    @GetMapping("/genres")
    public java.util.List<BeatGenre> getGenres() {
        return beatGenreRepository.findAll(org.springframework.data.domain.Sort.by("name").ascending());
    }

    @GetMapping("/{beatId}/processing")
    public java.util.List<com.studioos.server.beatmarketplace.dto.BeatProcessingStatusResponse> getProcessingStatus(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        return beatService.getProcessingStatus(producer.getId(), beatId);
    }

    @PostMapping
    public BeatUploadSessionResponse createBeat(
            @AuthenticationPrincipal User producer,
            @Valid @RequestBody CreateBeatRequest request) {
        return beatService.createDraftAndUploadSessions(producer.getId(), request);
    }

    @PostMapping("/{beatId}/upload-complete")
    public BeatUploadCompleteResponse completeUpload(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        return beatService.completeUpload(producer.getId(), beatId);
    }

    @PutMapping("/{beatId}")
    public ResponseEntity<Void> updateBeat(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId,
            @Valid @RequestBody UpdateBeatRequest request) {
        beatService.updateBeat(producer.getId(), beatId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{beatId}")
    public ResponseEntity<Void> archiveBeat(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        beatService.archiveBeat(producer.getId(), beatId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{beatId}/upload")
    public ResponseEntity<Void> cancelUpload(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        beatService.cancelUpload(producer.getId(), beatId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{beatId}/permanent")
    public ResponseEntity<Void> deleteArchivedBeat(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        beatService.deleteArchivedBeat(producer.getId(), beatId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{beatId}/processing/retry")
    public ResponseEntity<Void> retryProcessing(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        beatService.retryProcessing(producer.getId(), beatId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{beatId}/upload-sessions/refresh")
    public RefreshUploadSessionResponse refreshUploadSessions(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        return beatService.refreshUploadSessions(producer.getId(), beatId);
    }
}
