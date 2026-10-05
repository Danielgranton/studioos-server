package com.studioos.server.payment;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByBookingId(String bookingId);

    List<Transaction> findByStudioId(String studioId);
    List<Transaction> findByStudioIdOrderByCreatedAtDesc(String studioId);
    boolean existsByStudioId(String studioId);

    List<Transaction> findByUserId(Integer userId);

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByStudioIdAndType(String studioId, TransactionType type);

    Optional<Transaction> findByMpesaCheckoutRequestId(String mpesaCheckoutRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.mpesaCheckoutRequestId = :checkoutId")
    Optional<Transaction> findByCheckoutRequestIdForUpdate(@Param("checkoutId") String checkoutId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :transactionId")
    Optional<Transaction> findByIdForUpdate(@Param("transactionId") String transactionId);

    @Query("SELECT t.id FROM Transaction t WHERE t.type = :type AND t.status = :status "
            + "AND t.mpesaCheckoutRequestId IS NOT NULL AND t.createdAt < :createdBefore "
            + "AND (t.mpesaStatusCheckedAt IS NULL OR t.mpesaStatusCheckedAt < :checkedBefore) "
            + "ORDER BY t.createdAt ASC")
    List<String> findReconciliationCandidates(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("checkedBefore") LocalDateTime checkedBefore,
            Pageable pageable);

    org.springframework.data.domain.Page<Transaction> findByMpesaReviewRequiredTrueAndStatusOrderByMpesaReviewFlaggedAtAsc(
            TransactionStatus status, Pageable pageable);
}
