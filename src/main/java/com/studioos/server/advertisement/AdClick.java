package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_clicks")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdClick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String advertisementId;

    @Column(nullable = false)
    private String campaignId;

    private Integer userId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime clickedAt;
}