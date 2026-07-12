package com.studioos.server.advertisement.analytics;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.advertisement.Advertisement;
import com.studioos.server.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "advertisement_impressions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String advertisementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisementId", insertable = false, updatable = false)
    private Advertisement advertisement;

    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    private String sessionId;
    private String device;
    private String ip;

    @CreatedDate
    @Column(nullable = false , updatable = false)
    private LocalDateTime timestamp;

}