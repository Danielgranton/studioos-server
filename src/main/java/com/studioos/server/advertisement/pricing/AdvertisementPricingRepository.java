package com.studioos.server.advertisement.pricing;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementPricingRepository extends JpaRepository<AdvertisementPricing, String> {
    Optional<AdvertisementPricing> findByAdvertisementId(String advertisementId);
}