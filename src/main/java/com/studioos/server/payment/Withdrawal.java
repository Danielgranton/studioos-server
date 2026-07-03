package com.studioos.server.payment;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.WithdrawalStatus;
import com.studioos.server.studio.Studio;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "withdrawals")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String studioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studioId", insertable = false, updatable = false)
    private Studio studio;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;

    // Set once the payout transaction is created
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactionId", insertable = false, updatable = false)
    private Transaction transaction;

    private String mpesaReceiptNumber;
    private String mpesaPhoneNumber;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // ─── Audit ───
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}