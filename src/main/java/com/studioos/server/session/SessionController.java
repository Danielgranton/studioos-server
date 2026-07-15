package com.studioos.server.session;

import com.studioos.server.session.dto.RecordingSessionResponse;
import com.studioos.server.session.dto.CreateSessionDeliverableRequest;
import com.studioos.server.session.dto.SessionDeliverableUploadSessionResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final RecordingSessionService recordingSessionService;
    private final AttendanceService attendanceService;
    private final DeliverableService deliverableService;
    private final RevisionService revisionService;
    private final SessionTimelineService timelineService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> getSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RecordingSessionResponse>>> getMySessions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getMySessions(currentUser)));
    }

    @PostMapping("/{sessionId}/start")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> startSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.startSession(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> pauseSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.pauseSession(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/resume")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> resumeSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.resumeSession(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/recording/complete")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> finishRecording(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.finishRecording(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/mix/start")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> startMixing(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.startMixing(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/mix/finish")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> finishMixing(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.finishMixing(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/master/start")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> startMastering(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.startMastering(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> completeSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.completeSession(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<ApiResponse<RecordingSessionResponse>> cancelSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        recordingSessionService.cancelSession(currentUser, sessionId, details);
        return ResponseEntity.ok(ApiResponse.success(recordingSessionService.getSession(currentUser, sessionId)));
    }

    @PostMapping("/{sessionId}/attendance/check-in")
    public ResponseEntity<ApiResponse<SessionAttendance>> checkIn(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") boolean late
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkIn(sessionId, currentUser.getId(), role, late)));
    }

    @PostMapping("/{sessionId}/attendance/check-out")
    public ResponseEntity<ApiResponse<SessionAttendance>> checkOut(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "") String details
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkOut(sessionId, currentUser.getId(), details)));
    }

    @PostMapping("/{sessionId}/attendance/no-show")
    public ResponseEntity<ApiResponse<SessionAttendance>> markNoShow(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @RequestParam String role,
            @RequestParam(defaultValue = "") String details
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.markNoShow(sessionId, currentUser.getId(), role, details)));
    }

    @GetMapping("/{sessionId}/attendance")
    public ResponseEntity<ApiResponse<List<SessionAttendance>>> getAttendance(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId
    ) {
        recordingSessionService.getSession(currentUser, sessionId);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAttendance(sessionId)));
    }

    @GetMapping("/{sessionId}/timeline")
    public ResponseEntity<ApiResponse<List<SessionTimelineEntry>>> getTimeline(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId
    ) {
        recordingSessionService.getSession(currentUser, sessionId);
        return ResponseEntity.ok(ApiResponse.success(timelineService.getTimeline(sessionId)));
    }

    @GetMapping("/{sessionId}/deliverables")
    public ResponseEntity<ApiResponse<List<SessionDeliverable>>> getDeliverables(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId
    ) {
        recordingSessionService.getSession(currentUser, sessionId);
        return ResponseEntity.ok(ApiResponse.success(deliverableService.listDeliverables(sessionId)));
    }

    @PostMapping("/{sessionId}/deliverables/upload-session")
    public ResponseEntity<ApiResponse<SessionDeliverableUploadSessionResponse>> createDeliverableUploadSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @Valid @RequestBody CreateSessionDeliverableRequest request
    ) {
        String objectKey = "sessions/%s/deliverables/%s/%s_original".formatted(
                sessionId,
                request.getType().name().toLowerCase(),
                java.util.UUID.randomUUID());
        SessionDeliverableUploadSessionResponse response = deliverableService.startUpload(
                currentUser,
                sessionId,
                request.getType(),
                objectKey,
                request.getContentType(),
                request.getDuration());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{sessionId}/deliverables/{deliverableId}/complete")
    public ResponseEntity<ApiResponse<SessionDeliverable>> completeDeliverableUpload(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId,
            @PathVariable String deliverableId
    ) {
        return ResponseEntity.ok(ApiResponse.success(deliverableService.completeUpload(currentUser, sessionId, deliverableId)));
    }

    @GetMapping("/{sessionId}/revisions")
    public ResponseEntity<ApiResponse<List<SessionRevision>>> getRevisions(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String sessionId
    ) {
        recordingSessionService.getSession(currentUser, sessionId);
        return ResponseEntity.ok(ApiResponse.success(revisionService.getRevisions(sessionId)));
    }
}
