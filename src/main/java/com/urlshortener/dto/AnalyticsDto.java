package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDto {
    private Long urlId;
    private String originalUrl;
    private String shortUrl;
    private Long totalClicks;
    private List<Map<String, Object>> clicksPerDay;
    private List<Map<String, Object>> clicksByCountry;
    private List<Map<String, Object>> clicksByBrowser;
    private List<Map<String, Object>> clicksByPlatform;
    private List<Map<String, Object>> clicksByReferrer;
    private String shortCode;
} 