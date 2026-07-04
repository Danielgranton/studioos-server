package com.studioos.server.beatmarketplace;

import com.studioos.server.beatmarketplace.dto.BeatUploadCompleteResponse;
import com.studioos.server.beatmarketplace.dto.BeatUploadSessionResponse;
import com.studioos.server.beatmarketplace.dto.CreateBeatRequest;
import com.studioos.server.beatmarketplace.dto.RefreshUploadSessionResponse;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatController {

    private final BeatService beatService;

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

    @PostMapping("/{beatId}/upload-sessions/refresh")
    public RefreshUploadSessionResponse refreshUploadSessions(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId) {
        return beatService.refreshUploadSessions(producer.getId(), beatId);
    }
}