package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.payment.Transaction;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "beat_purchases")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String beatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatId", insertable = false, updatable = false)
    private Beat beat;

    @Column(nullable = false)
    private Integer buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyerId", insertable = false, updatable = false)
    private User buyer;

    @Column(nullable = false)
    private String licenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licenseId", insertable = false, updatable = false)
    private BeatLicense license;

    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactionId", insertable = false, updatable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BeatPaymentStatus status;

    @Builder.Default
    @Column(name = "is_exclusive", nullable = false)
    private Boolean isExclusive = false;

    @Builder.Default
    private Integer downloadCount = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime purchasedAt;
}