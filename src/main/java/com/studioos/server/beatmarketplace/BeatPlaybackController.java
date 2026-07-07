package com.studioos.server.beatmarketplace;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.beatmarketplace.dto.BeatDownloadResponse;
import com.studioos.server.beatmarketplace.dto.BeatPreviewResponse;
import com.studioos.server.user.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatPlaybackController {

    private final BeatPlaybackService beatPlaybackService;

    @GetMapping("/{beatId}/preview")
    public BeatPreviewResponse getPreview(
            @PathVariable String beatId,
            @AuthenticationPrincipal User user) {
        Integer userId = (user != null) ? user.getId() : null;
        return beatPlaybackService.getPreviewUrl(beatId, userId);
    }

    @GetMapping("/{beatId}/download")
    public BeatDownloadResponse getDownload(
        @PathVariable String beatId,
        @AuthenticationPrincipal User buyer,
        HttpServletRequest request) {

    if (buyer == null) {
        throw new SecurityException("You must be logged in to download a purchased beat");
    }

    return beatPlaybackService.getDownloadUrl(beatId, buyer.getId(), request.getRemoteAddr());
    }
}