package com.studioos.server.beatmarketplace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.studioos.server.beatmarketplace.BeatStorageConstants.MEDIA_BUCKET;
import com.studioos.server.beatmarketplace.dto.BeatDownloadResponse;
import com.studioos.server.beatmarketplace.dto.BeatPreviewResponse;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatPlaybackService {

    private static final int PREVIEW_URL_EXPIRY_SECONDS = 300; 
    private static final int DOWNLOAD_URL_EXPIRY_SECONDS = 600; 

    private final BeatRepository beatRepository;
    private final BeatPlayHistoryRepository beatPlayHistoryRepository;
    private final PresignedUrlService presignedUrlService;
    private final BeatPurchaseRepository beatPurchaseRepository;
    private final BeatDownloadRepository beatDownloadRepository;

    @Transactional
    public BeatPreviewResponse getPreviewUrl(String beatId, Integer userId) {

        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));

        if (beat.getStatus() != BeatStatus.READY) {
            throw new IllegalStateException("Beat is not ready for playback: " + beat.getStatus());
        }

        if (beat.getVisibility() == BeatVisibility.PRIVATE) {
            throw new SecurityException("This beat is private and cannot be previewed");
        }

        if (beat.getPreviewUrl() == null || beat.getPreviewUrl().isBlank()) {
            throw new IllegalStateException("No preview available for beat: " + beatId);
        }

        String signedUrl = presignedUrlService.generateDownloadUrl(
                MEDIA_BUCKET, beat.getPreviewUrl(), PREVIEW_URL_EXPIRY_SECONDS);

        recordPlay(beat, userId);

        return BeatPreviewResponse.builder()
                .beatId(beat.getId())
                .previewUrl(signedUrl)
                .expiresInSeconds(PREVIEW_URL_EXPIRY_SECONDS)
                .build();
    }

    private void recordPlay(Beat beat, Integer userId) {
        // NOTE: increments on every preview-URL request, not on confirmed listen completion.
        // A single real play may trigger multiple requests (signed URL expiry mid-listen,
        // page refresh, player retry) — this overcounts relative to "true" plays.
        // Revisit with a frontend-reported playback-duration signal if accurate counts matter later.

        BeatPlayHistory history = BeatPlayHistory.builder()
                .beatId(beat.getId())
                .userId(userId) // nullable — anonymous plays counted per earlier decision
                .build();
        beatPlayHistoryRepository.save(history);

        beat.setPlayCount(beat.getPlayCount() + 1);
        beatRepository.save(beat);
    }

    public BeatDownloadResponse getDownloadUrl(String beatId, Integer buyerId, String ipAddress) {

        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));

        BeatPurchase purchase = beatPurchaseRepository.findByBeatIdAndBuyerIdAndStatus(
                        beatId, buyerId, BeatPaymentStatus.PAID)
                .orElseThrow(() -> new SecurityException("You have not purchased a license for this beat"));

        if (beat.getAudioUrl() == null || beat.getAudioUrl().isBlank()) {
            throw new IllegalStateException("No downloadable audio available for beat: " + beatId);
        }

        String signedUrl = presignedUrlService.generateDownloadUrl(
                MEDIA_BUCKET, beat.getAudioUrl(), DOWNLOAD_URL_EXPIRY_SECONDS);

        recordDownload(purchase, beat, ipAddress);

        return BeatDownloadResponse.builder()
                .beatId(beat.getId())
                .downloadUrl(signedUrl)
                .expiresInSeconds(DOWNLOAD_URL_EXPIRY_SECONDS)
                .build();
    }

    @Transactional
    private void recordDownload(BeatPurchase purchase, Beat beat, String ipAddress) {
        BeatDownload download = BeatDownload.builder()
                .purchaseId(purchase.getId())
                .ipAddress(ipAddress)
                .build();
        beatDownloadRepository.save(download);

        purchase.setDownloadCount(purchase.getDownloadCount() + 1);
        beatPurchaseRepository.save(purchase);

        beat.setDownloadCount(beat.getDownloadCount() + 1);
        beatRepository.save(beat);
    }
}