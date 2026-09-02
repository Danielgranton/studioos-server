package com.studioos.server.shared.audit;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.studioos.server.payment.AuditLog;
import com.studioos.server.payment.AuditLogRepository;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.user.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountAuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(AuditEventType eventType, User user, String description) {
        RequestMetadata metadata = requestMetadata();
        auditLogRepository.save(AuditLog.builder()
                .eventType(eventType)
                .entityId(String.valueOf(user.getId()))
                .entityType("User")
                .userId(user.getId())
                .description(description)
                .ipAddress(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .build());
    }

    private RequestMetadata requestMetadata() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) return new RequestMetadata(null, null);
        HttpServletRequest request = servletAttributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ipAddress = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return new RequestMetadata(ipAddress, request.getHeader("User-Agent"));
    }

    private record RequestMetadata(String ipAddress, String userAgent) {}
}
