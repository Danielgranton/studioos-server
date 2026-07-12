package com.studioos.server.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.WithdrawalStatus;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, String> {

    List<Withdrawal> findByStudioId(String studioId);
    boolean existsByStudioId(String studioId);

    List<Withdrawal> findByStudioIdAndStatus(String studioId, WithdrawalStatus status);

    List<Withdrawal> findByStatus(WithdrawalStatus status);
}
