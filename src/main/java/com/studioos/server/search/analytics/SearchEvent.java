package com.studioos.server.search.analytics;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.studioos.server.shared.enums.SearchEntityType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "search_events")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SearchEntityType entityType;

    private String query;  

    private Integer userId;  

    @Column(nullable = false)
    private Integer resultCount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}