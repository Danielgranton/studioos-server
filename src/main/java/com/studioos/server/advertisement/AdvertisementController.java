package com.studioos.server.advertisement;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.studioos.server.advertisement.campaign.AdCampaignService;
import com.studioos.server.advertisement.campaign.dto.CampaignResponse;
import com.studioos.server.advertisement.campaign.dto.CreateCampaignRequest;
import com.studioos.server.advertisement.dto.AdUploadCompleteResponse;
import com.studioos.server.advertisement.dto.AdUploadSessionResponse;
import com.studioos.server.advertisement.dto.CreateAdvertisementRequest;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdCampaignService adCampaignService;
    private final AdvertisementUploadService advertisementUploadService;

    @PostMapping("/campaigns")
    public CampaignResponse createCampaign(
            @AuthenticationPrincipal User advertiser,
            @Valid @RequestBody CreateCampaignRequest request) {
        return adCampaignService.createCampaign(advertiser.getId(), request);
    }

    @PostMapping("/campaigns/{campaignId}/advertisements")
    public AdUploadSessionResponse createAdvertisement(
            @AuthenticationPrincipal User advertiser,
            @PathVariable String campaignId,
            @Valid @RequestBody CreateAdvertisementRequest request) {
        return advertisementUploadService.createAdvertisementAndUploadSession(advertiser.getId(), campaignId, request);
    }

    @PostMapping("/advertisements/{advertisementId}/upload-complete")
    public AdUploadCompleteResponse completeUpload(
            @AuthenticationPrincipal User advertiser,
            @PathVariable String advertisementId) {
        return advertisementUploadService.completeUpload(advertiser.getId(), advertisementId);
    }

    @PostMapping("/advertisements/{advertisementId}/upload-sessions/refresh")
    public AdUploadSessionResponse refreshUpload(
            @AuthenticationPrincipal User advertiser,
            @PathVariable String advertisementId) {
        return advertisementUploadService.refreshUploadSession(advertiser.getId(), advertisementId);
    }
}