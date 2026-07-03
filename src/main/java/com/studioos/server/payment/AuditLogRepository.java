package com.studioos.server.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.AuditEventType;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByEntityId(String entityId);

    List<AuditLog> findByEventType(AuditEventType eventType);
}