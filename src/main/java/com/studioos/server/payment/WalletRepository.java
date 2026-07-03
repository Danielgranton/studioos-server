package com.studioos.server.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.WalletType;

public interface WalletRepository extends JpaRepository<Wallet, String> {

    Optional<Wallet> findByStudioId(String studioId);

    Optional<Wallet> findByType(WalletType type);
}