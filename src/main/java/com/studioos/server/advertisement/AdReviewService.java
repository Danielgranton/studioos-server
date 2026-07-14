package com.studioos.server.advertisement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.studioos.server.search.event.AdvertisementPublishedEvent;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdReviewService {

    private final AdvertisementRepository advertisementRepository;
    private final AdNotificationService adNotificationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void approve(User admin, String advertisementId) {
        Advertisement ad = getPendingReview(admin, advertisementId);
        ad.setStatus(AdCreativeStatus.READY);
        advertisementRepository.save(ad);
        adNotificationService.notifyAdApproved(ad);
        applicationEventPublisher.publishEvent(new AdvertisementPublishedEvent(ad.getId()));
    }

    @Transactional
    public void reject(User admin, String advertisementId) {
        Advertisement ad = getPendingReview(admin, advertisementId);
        ad.setStatus(AdCreativeStatus.REJECTED);
        advertisementRepository.save(ad);
        adNotificationService.notifyAdRejected(ad);
    }

    private Advertisement getPendingReview(User admin, String advertisementId) {
        if (admin.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Only admins can review advertisements");
        }

        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> StudioosException.notFound("Advertisement not found"));

        if (ad.getStatus() != AdCreativeStatus.PENDING_REVIEW) {
            throw StudioosException.badRequest("Advertisement is not pending review: " + ad.getStatus());
        }

        return ad;
    }
}
