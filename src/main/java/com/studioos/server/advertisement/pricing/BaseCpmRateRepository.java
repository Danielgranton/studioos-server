package com.studioos.server.advertisement.pricing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.studioos.server.shared.enums.AdCreativeType;

public interface BaseCpmRateRepository extends JpaRepository<BaseCpmRate, String> {
    Optional<BaseCpmRate> findByMediaTypeAndActiveTrue(AdCreativeType mediaType);
}