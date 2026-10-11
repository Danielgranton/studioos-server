package com.studioos.server.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.campaign.AdCampaignService;
import com.studioos.server.advertisement.campaign.dto.CreateCampaignRequest;
import com.studioos.server.advertisement.campaign.dto.InitiateCampaignPaymentRequest;
import com.studioos.server.advertisement.dto.CreateAdvertisementRequest;
import com.studioos.server.advertisement.pricing.AdvertisementPriceResult;
import com.studioos.server.advertisement.pricing.AdvertisementPricing;
import com.studioos.server.advertisement.pricing.AdvertisementPricingRepository;
import com.studioos.server.advertisement.pricing.AdvertisementPricingService;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.payment.Transaction;
import com.studioos.server.notification.NotificationOutboxProcessor;
import com.studioos.server.search.index.SearchIndexService;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;
import com.studioos.server.shared.enums.AdImpressionVerificationStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.events.TransactionResolvedEvent;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.user.User;

@SpringBootTest(classes = com.studioos.server.StudioosServerApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:ad_workflow_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-key-with-enough-length-for-jjwt-signing",
        "internal.service.api-key=test-internal-service-key",
        "MAIL_USERNAME=test@example.com",
        "MAIL_PASSWORD=test-password",
        "MPESA_CONSUMER_KEY=test-consumer-key",
        "MPESA_CONSUMER_SECRET=test-consumer-secret",
        "MPESA_SHORTCODE=123456",
        "MPESA_PASSKEY=test-passkey",
        "MPESA_CALLBACK_URL=http://localhost/callback",
        "MPESA_TIMEOUT_URL=http://localhost/timeout",
        "africastalking.username=test-user",
        "africastalking.apikey=test-api-key",
        "storage.s3.bucket=test-bucket",
        "opensearch.host=localhost",
        "opensearch.port=9200",
        "search.indexing.init-on-startup=false",
        "media.callback.grpc.enabled=false",
        "bootstrap.super-admin.email=",
        "bootstrap.super-admin.password="
})
class AdvertisingWorkflowIntegrationTest {

    @Autowired private AdCampaignService campaignService;
    @Autowired private AdvertisementUploadService uploadService;
    @Autowired private AdReviewService reviewService;
    @Autowired private AdDeliveryService deliveryService;
    @Autowired private AdImpressionConfirmationService impressionConfirmationService;
    @Autowired private AdCampaignRepository campaignRepository;
    @Autowired private AdBudgetRepository budgetRepository;
    @Autowired private AdvertisementRepository advertisementRepository;
    @Autowired private AdUploadSessionRepository uploadSessionRepository;
    @Autowired private AdMediaProcessingJobRepository mediaJobRepository;
    @Autowired private AdvertisementPricingRepository pricingRepository;
    @Autowired private AdImpressionRepository impressionRepository;
    @Autowired private AdImpressionVerificationRepository verificationRepository;

    @MockBean private AdvertisementPricingService pricingService;
    @MockBean private MediaProcessingClient mediaProcessingClient;
    @MockBean private PresignedUrlService presignedUrlService;
    @MockBean private PaymentService paymentService;
    @MockBean private AdNotificationService adNotificationService;
    @MockBean private NotificationOutboxProcessor notificationOutboxProcessor;
    @MockBean private SearchIndexService searchIndexService;
    @MockBean private JavaMailSender mailSender;
    @MockBean private StringRedisTemplate redisTemplate;

    @Test
    void uploadedCreativeIsProcessedReviewedPaidAndBilledOnlyAfterDeliveryIsConfirmed() {
        int advertiserId = 731;
        LocalDateTime now = LocalDateTime.now();
        CreateCampaignRequest campaignRequest = new CreateCampaignRequest();
        campaignRequest.setTitle("Integration campaign");
        campaignRequest.setPlacement(AdPlacement.EXPLORE_PAGE);
        campaignRequest.setStartDate(now.minusMinutes(1));
        campaignRequest.setEndDate(now.plusDays(3));
        campaignRequest.setTotalBudget(5000);

        var createdCampaign = campaignService.createCampaign(advertiserId, campaignRequest);
        String campaignId = createdCampaign.getCampaignId();
        AtomicInteger jobsIssued = new AtomicInteger();
        when(presignedUrlService.generateUploadUrl(any(), any(), any(), anyInt()))
                .thenReturn("https://storage.test/upload");
        when(mediaProcessingClient.submitJob(any(), any(), any()))
                .thenAnswer(invocation -> "media-job-" + jobsIssued.incrementAndGet());
        when(pricingService.priceAndPersist(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    String campaign = invocation.getArgument(0);
                    String ad = invocation.getArgument(1);
                    pricingRepository.save(AdvertisementPricing.builder()
                            .campaignId(campaign)
                            .advertisementId(ad)
                            .baseCpm(100)
                            .placementMultiplier(1.0)
                            .placementRuleFound(true)
                            .targetingMultiplier(1.0)
                            .targetingRuleFound(true)
                            .durationMultiplier(1.0)
                            .durationRuleFound(true)
                            .budgetMultiplier(1.0)
                            .budgetRuleFound(true)
                            .promotionMultiplier(1.0)
                            .promotionRuleFound(true)
                            .finalCpm(100.0)
                            .estimatedImpressions(50000L)
                            .currency("KES")
                            .build());
                    return AdvertisementPriceResult.builder()
                            .baseCpm(100)
                            .placementMultiplier(1.0)
                            .placementRuleFound(true)
                            .targetingMultiplier(1.0)
                            .targetingRuleFound(true)
                            .durationMultiplier(1.0)
                            .durationRuleFound(true)
                            .budgetMultiplier(1.0)
                            .budgetRuleFound(true)
                            .promotionMultiplier(1.0)
                            .promotionRuleFound(true)
                            .finalCpm(100.0)
                            .estimatedImpressions(50000L)
                            .currency("KES")
                            .build();
                });

        CreateAdvertisementRequest adRequest = new CreateAdvertisementRequest();
        adRequest.setType(AdCreativeType.IMAGE);
        adRequest.setHeadline("Integration creative");
        adRequest.setContentType("image/jpeg");
        var upload = uploadService.createAdvertisementAndUploadSession(advertiserId, campaignId, adRequest);
        AdUploadSession session = uploadSessionRepository.findByAdvertisementId(upload.getAdvertisementId())
                .get(0);
        when(presignedUrlService.objectExists("test-bucket", session.getObjectKey())).thenReturn(true);

        var uploadCompleted = uploadService.completeUpload(advertiserId, upload.getAdvertisementId());
        assertThat(uploadCompleted.getStatus()).isEqualTo(AdCreativeStatus.PROCESSING.name());
        assertThat(mediaJobRepository.findByAdvertisementId(upload.getAdvertisementId())).hasSize(3);

        List<AdMediaProcessingJob> jobs = mediaJobRepository.findByAdvertisementId(upload.getAdvertisementId());
        for (AdMediaProcessingJob job : jobs) {
            String resultReference = switch (job.getOperation()) {
                case IMAGE_WEBP -> "s3://test-bucket/ads/processed/final.webp";
                case IMAGE_THUMBNAIL -> "s3://test-bucket/ads/processed/thumb.webp";
                default -> "s3://test-bucket/ads/processed/resized.jpg";
            };
            assertThat(uploadService.applyMediaJobResult(MediaJobResult.builder()
                    .jobId(job.getExternalJobId())
                    .status(MediaJobStatus.SUCCESS)
                    .resultReference(resultReference)
                    .build())).isTrue();
        }
        Advertisement processedAd = advertisementRepository.findById(upload.getAdvertisementId()).orElseThrow();
        assertThat(processedAd.getStatus()).isEqualTo(AdCreativeStatus.PENDING_REVIEW);
        assertThat(processedAd.getMediaUrl()).isEqualTo("s3://test-bucket/ads/processed/final.webp");

        reviewService.approve(User.builder().role(Role.SUPER_ADMIN).build(), processedAd.getId());
        assertThat(advertisementRepository.findById(processedAd.getId()).orElseThrow().getStatus())
                .isEqualTo(AdCreativeStatus.READY);

        Transaction payment = Transaction.builder()
                .id("ad-payment-" + campaignId)
                .type(TransactionType.AD_CAMPAIGN)
                .status(TransactionStatus.PENDING)
                .amount(5000)
                .build();
        when(paymentService.initiateAdCampaignPayment(eq(advertiserId), any(), eq(5000), any(), any()))
                .thenReturn(payment);
        InitiateCampaignPaymentRequest paymentRequest = new InitiateCampaignPaymentRequest();
        paymentRequest.setPhoneNumber("+254712345678");
        campaignService.initiatePayment(advertiserId, campaignId, paymentRequest);
        campaignService.onTransactionResolved(TransactionResolvedEvent.builder()
                .transactionId(payment.getId())
                .type(TransactionType.AD_CAMPAIGN)
                .success(true)
                .build());

        assertThat(campaignRepository.findById(campaignId).orElseThrow().getPaymentStatus())
                .isEqualTo(AdPaymentStatus.PAID);
        assertThat(campaignRepository.findById(campaignId).orElseThrow().getStatus())
                .isEqualTo(AdCampaignStatus.ACTIVE);
        when(presignedUrlService.generateDownloadUrl("test-bucket", "ads/processed/final.webp", 300))
                .thenReturn("https://storage.test/final.webp");
        when(presignedUrlService.generateDownloadUrl("test-bucket", "ads/processed/thumb.webp", 300))
                .thenReturn("https://storage.test/thumb.webp");

        var delivered = deliveryService.selectAdvertisement(AdPlacement.EXPLORE_PAGE, null).orElseThrow();
        assertThat(delivered.getAdvertisementId()).isEqualTo(processedAd.getId());
        assertThat(delivered.getMediaUrl()).isEqualTo("https://storage.test/final.webp");
        assertThat(impressionRepository.count()).isZero();
        assertThat(budgetRepository.findByCampaignId(campaignId).orElseThrow().getRemainingBudget()).isEqualTo(5000);

        assertThat(impressionConfirmationService.confirm(delivered.getImpressionToken(), null).isRecorded()).isTrue();
        assertThat(impressionRepository.count()).isEqualTo(1);
        assertThat(verificationRepository.findById(delivered.getImpressionToken()).orElseThrow().getStatus())
                .isEqualTo(AdImpressionVerificationStatus.CONFIRMED);
        assertThat(budgetRepository.findByCampaignId(campaignId).orElseThrow().getRemainingBudget()).isEqualTo(4999);
        assertThat(budgetRepository.findByCampaignId(campaignId).orElseThrow().getSpentBudget()).isEqualTo(1);
    }
}
