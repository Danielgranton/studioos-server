package com.studioos.server.studio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "catalog_service_id")
    private String catalogServiceId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "included_in_production_package", nullable = false)
    @Builder.Default
    private boolean includedInProductionPackage = false;

    @Column(name = "service_price")
    private Integer price;

    @Column(nullable = false)
    private String studioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studioId", insertable = false, updatable = false)
    private Studio studio;
}
