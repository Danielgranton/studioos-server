package com.studioos.server.session;

import java.util.Arrays;

import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.SessionStatus;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionAccessService {

    public void assertCanView(RecordingSession session, User currentUser) {
        if (currentUser == null) {
            throw StudioosException.unauthorized("Authentication is required");
        }
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        boolean isParticipant = currentUser.getId().equals(session.getArtistId())
                || currentUser.getId().equals(session.getProducerId())
                || currentUser.getId().equals(session.getEngineerId());
        if (!isParticipant) {
            throw StudioosException.forbidden("You cannot view this session");
        }
    }

    public void assertCanManageRecording(RecordingSession session, User currentUser) {
        assertCanView(session, currentUser);
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        if (!isAssignedProducerOrEngineer(session, currentUser) && !currentUser.getId().equals(session.getArtistId())) {
            throw StudioosException.forbidden("You cannot manage this recording session");
        }
    }

    public void assertCanManageProduction(RecordingSession session, User currentUser) {
        assertCanView(session, currentUser);
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        if (!isAssignedProducerOrEngineer(session, currentUser)) {
            throw StudioosException.forbidden("You cannot manage this production stage");
        }
    }

    public void assertCanCancel(RecordingSession session, User currentUser) {
        assertCanView(session, currentUser);
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        if (!isAssignedProducerOrEngineer(session, currentUser) && !currentUser.getId().equals(session.getArtistId())) {
            throw StudioosException.forbidden("You cannot cancel this session");
        }
    }

    public void assertStatus(RecordingSession session, SessionStatus... allowedStatuses) {
        if (Arrays.stream(allowedStatuses).noneMatch(status -> status == session.getStatus())) {
            throw StudioosException.badRequest("Session is not in a valid state for this action");
        }
    }

    private boolean isAssignedProducerOrEngineer(RecordingSession session, User currentUser) {
        return (session.getProducerId() != null && session.getProducerId().equals(currentUser.getId()))
                || (session.getEngineerId() != null && session.getEngineerId().equals(currentUser.getId()));
    }
}
