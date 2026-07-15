package com.studioos.server.session;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.enums.RevisionStatus;
import com.studioos.server.shared.enums.SessionTimelineAction;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private final SessionRevisionRepository sessionRevisionRepository;
    private final SessionTimelineService timelineService;

    @Transactional
    public SessionRevision requestRevision(String sessionId, String deliverableId, Integer requestedBy, String comments) {
        SessionRevision revision = sessionRevisionRepository.save(SessionRevision.builder()
                .sessionId(sessionId)
                .deliverableId(deliverableId)
                .requestedBy(requestedBy)
                .comments(comments)
                .status(RevisionStatus.OPEN)
                .build());
        timelineService.recordEvent(sessionId, SessionTimelineAction.REVISION_REQUESTED, requestedBy, comments);
        return revision;
    }

    @Transactional
    public SessionRevision approveRevision(String revisionId, Integer performedBy) {
        SessionRevision revision = requireRevision(revisionId);
        revision.setStatus(RevisionStatus.DONE);
        revision = sessionRevisionRepository.save(revision);
        timelineService.recordEvent(revision.getSessionId(), SessionTimelineAction.REVISION_APPROVED, performedBy, null);
        return revision;
    }

    @Transactional
    public SessionRevision rejectRevision(String revisionId, Integer performedBy, String details) {
        SessionRevision revision = requireRevision(revisionId);
        revision.setStatus(RevisionStatus.REJECTED);
        revision = sessionRevisionRepository.save(revision);
        timelineService.recordEvent(revision.getSessionId(), SessionTimelineAction.REVISION_REJECTED, performedBy, details);
        return revision;
    }

    @Transactional
    public SessionRevision completeRevision(String revisionId, Integer performedBy) {
        SessionRevision revision = requireRevision(revisionId);
        revision.setStatus(RevisionStatus.DONE);
        revision = sessionRevisionRepository.save(revision);
        timelineService.recordEvent(revision.getSessionId(), SessionTimelineAction.REVISION_COMPLETED, performedBy, null);
        return revision;
    }

    @Transactional(readOnly = true)
    public List<SessionRevision> getRevisions(String sessionId) {
        return sessionRevisionRepository.findBySessionId(sessionId);
    }

    private SessionRevision requireRevision(String revisionId) {
        return sessionRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new IllegalArgumentException("Revision not found: " + revisionId));
    }
}
