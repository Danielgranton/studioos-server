package com.studioos.server.advertisement;

import java.util.List;

import org.springframework.stereotype.Service;

import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdNotificationService {

    private final NotificationServiceImpl notificationService;
    private final AdCampaignRepository adCampaignRepository;
    private final UserRepository userRepository;

    public void notifyCampaignPaymentResult(AdCampaign campaign, boolean success) {
        String title = success ? "Campaign payment succeeded" : "Campaign payment failed";
        String message = success
                ? "Your ad campaign \"" + campaign.getTitle() + "\" is paid and ready for scheduling."
                : "Payment for your ad campaign \"" + campaign.getTitle() + "\" failed or was reversed.";

        send(campaign.getAdvertiserId(),
                success ? NotificationType.AD_CAMPAIGN_PAYMENT_SUCCESS : NotificationType.AD_CAMPAIGN_PAYMENT_FAILED,
                title,
                message,
                campaign.getId());
    }

    public void notifyCampaignActivated(AdCampaign campaign) {
        send(campaign.getAdvertiserId(),
                NotificationType.AD_CAMPAIGN_LIVE,
                "Campaign is live",
                "Your ad campaign \"" + campaign.getTitle() + "\" is now active.",
                campaign.getId());
    }

    public void notifyAdApproved(Advertisement ad) {
        AdCampaign campaign = requireCampaign(ad.getCampaignId());
        send(campaign.getAdvertiserId(),
                NotificationType.ADVERTISEMENT_APPROVED,
                "Advertisement approved",
                "Your advertisement \"" + ad.getHeadline() + "\" has been approved and is ready to serve.",
                ad.getId());
    }

    public void notifyAdRejected(Advertisement ad) {
        AdCampaign campaign = requireCampaign(ad.getCampaignId());
        send(campaign.getAdvertiserId(),
                NotificationType.ADVERTISEMENT_REJECTED,
                "Advertisement rejected",
                "Your advertisement \"" + ad.getHeadline() + "\" was rejected during review.",
                ad.getId());
    }

    public void notifyAdProcessingCompleted(Advertisement ad) {
        AdCampaign campaign = requireCampaign(ad.getCampaignId());
        send(campaign.getAdvertiserId(),
                NotificationType.ADVERTISEMENT_PROCESSING_COMPLETED,
                "Advertisement processed",
                "Your advertisement \"" + ad.getHeadline() + "\" finished processing and is awaiting review.",
                ad.getId());
    }

    public void notifyAdProcessingFailed(Advertisement ad, String reason) {
        AdCampaign campaign = requireCampaign(ad.getCampaignId());
        send(campaign.getAdvertiserId(),
                NotificationType.ADVERTISEMENT_PROCESSING_FAILED,
                "Advertisement processing failed",
                "Your advertisement \"" + ad.getHeadline() + "\" failed processing."
                        + (reason == null || reason.isBlank() ? "" : " Reason: " + reason),
                ad.getId());
    }

    public void notifyAdNeedsReview(Advertisement ad) {
        List<User> admins = userRepository.findByRole(Role.SUPER_ADMIN);
        if (admins.isEmpty()) {
            log.warn("No SUPER_ADMIN users found to notify for advertisement review: {}", ad.getId());
            return;
        }

        for (User admin : admins) {
            send(admin.getId(),
                    NotificationType.ADVERTISEMENT_REVIEW_REQUIRED,
                    "Advertisement needs review",
                    "An advertisement titled \"" + ad.getHeadline() + "\" is waiting for review.",
                    ad.getId());
        }
    }

    private AdCampaign requireCampaign(String campaignId) {
        return adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));
    }

    private void send(Integer userId, NotificationType type, String title, String message, String relatedEntityId) {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setTitle(title);
        request.setMessage(message);
        request.setRelatedEntityId(relatedEntityId);
        notificationService.createNotification(request);
    }
}
