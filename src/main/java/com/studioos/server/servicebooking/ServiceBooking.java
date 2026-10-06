package com.studioos.server.servicebooking;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_bookings")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "provider_id", nullable = false)
    private Integer providerId;

    @Column(name = "provider_type", nullable = false, length = 16)
    private String providerType;

    @Column(name = "listing_id", nullable = false)
    private String listingId;

    @Column(name = "studio_id")
    private String studioId;

    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    @Column(name = "requester_id", nullable = false)
    private Integer requesterId;

    @Column(name = "preferred_date", nullable = false)
    private LocalDateTime preferredDate;

    @Column(name = "request_details", nullable = false, length = 2000)
    private String requestDetails;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceBookingStatus status = ServiceBookingStatus.PENDING;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
