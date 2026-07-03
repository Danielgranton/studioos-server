package com.studioos.server.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.EscrowStatus;

public interface EscrowRepository extends JpaRepository<Escrow, String> {

    Optional<Escrow> findByBookingId(String bookingId);

    Optional<Escrow> findByTransactionId(String transactionId);

    java.util.List<Escrow> findByStatus(EscrowStatus status);
}