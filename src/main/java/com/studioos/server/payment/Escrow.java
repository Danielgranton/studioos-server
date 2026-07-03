package com.studioos.server.payment;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.EscrowStatus;
import com.studioos.server.booking.Booking;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "escrows")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Escrow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", insertable = false, updatable = false)
    private Booking booking;

    @Column(nullable = false)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactionId", insertable = false, updatable = false)
    private Transaction transaction;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EscrowStatus status;

    @Column(nullable = false)
    private Integer amount;

    // ─── Audit ───
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}