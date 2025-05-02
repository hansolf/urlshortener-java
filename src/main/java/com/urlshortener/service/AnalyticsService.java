package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsDto;
import com.urlshortener.dto.analytics.ClickEventDto;
import com.urlshortener.model.Url;

public interface AnalyticsService {
    void recordClick(ClickEventDto clickEventDto);
    AnalyticsDto getAnalyticsForUrl(Long urlId);
    AnalyticsDto getAnalyticsForUrl(Url url);
} 