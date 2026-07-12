package com.studioos.server.advertisement.targeting;

import com.studioos.server.advertisement.campaign.AdCampaign;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_targeting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Targeting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String campaignId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaignId", insertable = false, updatable = false)
    private AdCampaign campaign;

    private String countries;
    private String cities;
    private String genres;
    private Integer ageMin;
    private Integer ageMax;
    private String gender;
    private String interests;
    private String devices;
}
