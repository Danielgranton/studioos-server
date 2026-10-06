package com.studioos.server.servicebooking;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, String> {
    List<ServiceBooking> findByRequesterIdOrderByCreatedAtDesc(Integer requesterId);
    List<ServiceBooking> findByProviderIdOrderByCreatedAtDesc(Integer providerId);
    Optional<ServiceBooking> findByTransactionId(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ServiceBooking b WHERE b.id = :id")
    Optional<ServiceBooking> findByIdForUpdate(@Param("id") String id);
}
