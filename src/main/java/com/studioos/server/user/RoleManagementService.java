package com.studioos.server.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.service.SessionService;
import com.studioos.server.auth.service.TokenService;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.audit.AccountAuditService;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.dto.UpdateRoleRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AccountAuditService accountAuditService;

    @Transactional
    public AuthResponse updateOwnRole(User currentUser, UpdateRoleRequest request) {
        if (currentUser == null) throw StudioosException.unauthorized("Authentication required");
        if (request == null || request.getRole() == null) throw StudioosException.badRequest("Role is required");
        if (request.getRole() == Role.ADMIN || request.getRole() == Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Privileged roles can only be assigned by an authorized administrator");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        if (user.getRole() == request.getRole()) return tokenService.issue(user);

        Role previousRole = user.getRole();
        user.setRole(request.getRole());
        userRepository.save(user);
        accountAuditService.record(AuditEventType.ROLE_CHANGED, user, "Role changed from " + previousRole + " to " + request.getRole());
        sessionService.logoutAllDevices(user);

        AuthResponse response = tokenService.issue(user);
        sessionService.recordSession(user, response.getRefreshToken());
        return response;
    }

    @Transactional
    public User updateUserRole(User actor, Integer targetUserId, UpdateRoleRequest request) {
        if (actor == null) throw StudioosException.unauthorized("Authentication required");
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Administrator access required");
        }
        if (request == null || request.getRole() == null) throw StudioosException.badRequest("Role is required");
        if (targetUserId == null) throw StudioosException.badRequest("Target user is required");
        if (actor.getId().equals(targetUserId)) {
            throw StudioosException.badRequest("Use the account role settings to change your own role");
        }
        if (actor.getRole() == Role.ADMIN
                && (request.getRole() == Role.ADMIN || request.getRole() == Role.SUPER_ADMIN)) {
            throw StudioosException.forbidden("Only a SUPER_ADMIN can assign privileged roles");
        }
        if (request.getRole() == Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("SUPER_ADMIN is managed through the bootstrap process");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        Role previousRole = target.getRole();
        if (previousRole == request.getRole()) return target;

        target.setRole(request.getRole());
        userRepository.save(target);
        sessionService.logoutAllDevices(target);
        accountAuditService.record(AuditEventType.ROLE_CHANGED, target,
                "Role changed by administrator from " + previousRole + " to " + request.getRole());
        return target;
    }
}
