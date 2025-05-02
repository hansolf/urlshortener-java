package com.urlshortener.repository;

import com.urlshortener.model.Analytics;
import com.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    List<Analytics> findByUrl(Url url);
    
    @Query("SELECT COUNT(a) FROM Analytics a WHERE a.url = :url")
    Long countByUrl(@Param("url") Url url);
    
    @Query("SELECT DATE(a.timestamp) as date, COUNT(a) as count FROM Analytics a WHERE a.url = :url GROUP BY DATE(a.timestamp) ORDER BY date")
    List<Map<String, Object>> getClicksPerDayByUrl(@Param("url") Url url);
    
    @Query("SELECT a.country as country, COUNT(a) as count FROM Analytics a WHERE a.url = :url GROUP BY a.country ORDER BY count DESC")
    List<Map<String, Object>> getClicksByCountryForUrl(@Param("url") Url url);
    
    @Query("SELECT a.browser as browser, COUNT(a) as count FROM Analytics a WHERE a.url = :url GROUP BY a.browser ORDER BY count DESC")
    List<Map<String, Object>> getClicksByBrowserForUrl(@Param("url") Url url);
    
    @Query("SELECT a.platform as platform, COUNT(a) as count FROM Analytics a WHERE a.url = :url GROUP BY a.platform ORDER BY count DESC")
    List<Map<String, Object>> getClicksByPlatformForUrl(@Param("url") Url url);
    
    @Query("SELECT a.referrer as referrer, COUNT(a) as count FROM Analytics a WHERE a.url = :url AND a.referrer IS NOT NULL GROUP BY a.referrer ORDER BY count DESC")
    List<Map<String, Object>> getClicksByReferrerForUrl(@Param("url") Url url);
    
    List<Analytics> findByUrlAndTimestampBetween(Url url, LocalDateTime start, LocalDateTime end);
} 