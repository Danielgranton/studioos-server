package com.studioos.server.advertisement.pricing;

import com.studioos.server.shared.enums.AdCreativeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "base_cpm_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseCpmRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private AdCreativeType mediaType;

    @Column(nullable = false)
    private Integer baseCpm;

    @Builder.Default
    private Boolean active = true;
}