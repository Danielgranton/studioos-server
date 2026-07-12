package com.studioos.server.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByBookingId(String bookingId);

    List<Transaction> findByStudioId(String studioId);
    boolean existsByStudioId(String studioId);

    List<Transaction> findByUserId(Integer userId);

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByStudioIdAndType(String studioId, TransactionType type);

    Optional<Transaction> findByMpesaCheckoutRequestId(String mpesaCheckoutRequestId);
}
