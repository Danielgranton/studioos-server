package com.studioos.server.payment;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.WalletType;
import com.studioos.server.studio.Studio;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WalletType type;

    // Null when type = PLATFORM
    private String studioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studioId", insertable = false, updatable = false)
    private Studio studio;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableBalance = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer pendingBalance = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedBalance = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer withdrawnBalance = 0;

    // ─── Audit ───
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
