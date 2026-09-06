package com.studioos.server.studio;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.studioos.server.auth.service.ProfileImageServiceClient;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.media.ResponsiveImageAsset;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.studio.dto.StudioMediaResponse;
import com.studioos.server.studio.dto.StudioVideoUploadResponse;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudioMediaService {
    private static final String IMAGE = "IMAGE";
    private static final String VIDEO = "VIDEO";
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private static final int UPLOAD_URL_EXPIRY_SECONDS = 900;

    private final StudioRepository studioRepository;
    private final StudioMediaRepository studioMediaRepository;
    private final ProfileImageServiceClient profileImageServiceClient;
    private final PresignedUrlService presignedUrlService;

    @Value("${storage.s3.bucket}")
    private String mediaBucket;

    @Transactional
    public StudioMediaResponse uploadImage(User currentUser, String studioId, MultipartFile file) {
        Studio studio = findOwnedStudio(currentUser, studioId);
        validateImage(file);
        long profileImageCount = studio.getProfileImage() == null ? 0 : 1;
        long galleryImageCount = studioMediaRepository.countByStudioIdAndMediaType(studioId, IMAGE);
        if (profileImageCount + galleryImageCount >= 5) {
            throw StudioosException.badRequest("A studio can have up to 5 gallery images including its profile image");
        }

        String mediaId = UUID.randomUUID().toString();
        try (var input = file.getInputStream()) {
            ResponsiveImageAsset image = profileImageServiceClient.processUploadedProfileImage(
                    input,
                    file.getSize(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    "studios/" + studio.getId() + "/gallery/" + mediaId,
                    "studio-gallery-" + studio.getId());
            if (image == null || image.getOriginalUrl() == null) {
                throw StudioosException.badRequest("Image processing did not return an image");
            }

            StudioMedia media = StudioMedia.builder()
                    .id(mediaId)
                    .studioId(studioId)
                    .mediaType(IMAGE)
                    .storageKey(image.getOriginalUrl())
                    .originalUrl(image.getOriginalUrl())
                    .largeUrl(image.getLargeUrl())
                    .mediumUrl(image.getMediumUrl())
                    .thumbnailUrl(image.getThumbnailUrl())
                    .displayOrder((int) studioMediaRepository.countByStudioIdAndMediaType(studioId, IMAGE))
                    .build();
            return toResponse(studioMediaRepository.save(media));
        } catch (IOException e) {
            throw StudioosException.badRequest("Could not read studio gallery image");
        }
    }

    @Transactional
    public StudioVideoUploadResponse createVideoUpload(User currentUser, String studioId, String contentType, long contentLength) {
        findOwnedStudio(currentUser, studioId);
        validateVideoMetadata(contentType, contentLength);
        ensureLimit(studioId, VIDEO, 1, "A studio can have only 1 gallery video");

        String mediaId = UUID.randomUUID().toString();
        String extension = switch (contentType) {
            case "video/webm" -> "webm";
            case "video/quicktime" -> "mov";
            default -> "mp4";
        };
        String key = "media/studios/%s/gallery/%s.%s".formatted(studioId, mediaId, extension);
        StudioMedia media = StudioMedia.builder()
                .id(mediaId)
                .studioId(studioId)
                .mediaType(VIDEO)
                .storageKey(key)
                .displayOrder(0)
                .build();
        studioMediaRepository.save(media);

        return StudioVideoUploadResponse.builder()
                .mediaId(mediaId)
                .uploadUrl(presignedUrlService.generateUploadUrl(
                        mediaBucket, key, contentType, UPLOAD_URL_EXPIRY_SECONDS))
                .expiresAt(java.time.Instant.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS).toString())
                .build();
    }

    @Transactional
    public StudioMediaResponse completeVideoUpload(User currentUser, String studioId, String mediaId) {
        findOwnedStudio(currentUser, studioId);
        StudioMedia media = studioMediaRepository.findById(mediaId)
                .filter(item -> studioId.equals(item.getStudioId()) && VIDEO.equals(item.getMediaType()))
                .orElseThrow(() -> StudioosException.notFound("Studio video upload not found"));
        if (!presignedUrlService.objectExists(mediaBucket, media.getStorageKey())) {
            throw StudioosException.badRequest("Studio video has not finished uploading");
        }
        return toResponse(media);
    }

    @Transactional
    public void deleteMedia(User currentUser, String studioId, String mediaId) {
        findOwnedStudio(currentUser, studioId);
        StudioMedia media = studioMediaRepository.findById(mediaId)
                .filter(item -> studioId.equals(item.getStudioId()))
                .orElseThrow(() -> StudioosException.notFound("Studio media not found"));
        studioMediaRepository.delete(media);
    }

    public List<StudioMediaResponse> getMedia(String studioId) {
        return studioMediaRepository.findByStudioIdOrderByDisplayOrderAscCreatedAtAsc(studioId)
                .stream().map(this::toResponse).toList();
    }

    private Studio findOwnedStudio(User currentUser, String studioId) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));
        if (!studio.getOwnerId().equals(currentUser.getId()) && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You do not own this studio");
        }
        return studio;
    }

    private void ensureLimit(String studioId, String type, int limit, String message) {
        if (studioMediaRepository.countByStudioIdAndMediaType(studioId, type) >= limit) {
            throw StudioosException.badRequest(message);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw StudioosException.badRequest("Studio gallery image is required");
        if (file.getSize() > MAX_IMAGE_BYTES) throw StudioosException.badRequest("Gallery images must not exceed 5 MB");
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(file.getContentType())) {
            throw StudioosException.badRequest("Only JPEG, PNG, and WebP gallery images are supported");
        }
    }

    private void validateVideoMetadata(String contentType, long contentLength) {
        if (contentLength < 1 || contentLength > MAX_VIDEO_BYTES) {
            throw StudioosException.badRequest("Studio videos must not exceed 100 MB");
        }
        if (!List.of("video/mp4", "video/webm", "video/quicktime").contains(contentType)) {
            throw StudioosException.badRequest("Only MP4, WebM, and MOV studio videos are supported");
        }
    }

    private StudioMediaResponse toResponse(StudioMedia media) {
        String original = media.getOriginalUrl() != null
                ? resolveReference(media.getOriginalUrl())
                : presignedUrlService.generateDownloadUrl(mediaBucket, media.getStorageKey(), 3600);
        return StudioMediaResponse.builder()
                .id(media.getId())
                .type(media.getMediaType())
                .url(original)
                .largeUrl(resolveReference(media.getLargeUrl()))
                .mediumUrl(resolveReference(media.getMediumUrl()))
                .thumbnailUrl(resolveReference(media.getThumbnailUrl()))
                .displayOrder(media.getDisplayOrder())
                .build();
    }

    private String resolveReference(String reference) {
        if (reference == null || !reference.startsWith("s3://")) return reference;
        String remainder = reference.substring("s3://".length());
        int separator = remainder.indexOf('/');
        if (separator <= 0 || separator == remainder.length() - 1) return reference;
        return presignedUrlService.generateDownloadUrl(
                remainder.substring(0, separator), remainder.substring(separator + 1), 3600);
    }
}
