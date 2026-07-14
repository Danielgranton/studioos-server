package com.studioos.server.search.analytics;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.studioos.server.shared.enums.SearchEntityType;

public interface SearchEventRepository extends JpaRepository<SearchEvent, String> {

    @Query("SELECT e.query AS query, COUNT(e) AS searchCount " +
           "FROM SearchEvent e " +
           "WHERE e.entityType = :entityType AND e.query IS NOT NULL AND e.query <> '' " +
           "AND e.createdAt >= :since " +
           "GROUP BY e.query ORDER BY COUNT(e) DESC")
    List<TopSearchProjection> findTopSearches(
            @Param("entityType") SearchEntityType entityType,
            @Param("since") LocalDateTime since);

    @Query("SELECT e.query AS query, COUNT(e) AS searchCount " +
           "FROM SearchEvent e " +
           "WHERE e.entityType = :entityType AND e.resultCount = 0 " +
           "AND e.query IS NOT NULL AND e.query <> '' AND e.createdAt >= :since " +
           "GROUP BY e.query ORDER BY COUNT(e) DESC")
    List<TopSearchProjection> findNoResultSearches(
            @Param("entityType") SearchEntityType entityType,
            @Param("since") LocalDateTime since);

    interface TopSearchProjection {
        String getQuery();
        Long getSearchCount();
    }
}