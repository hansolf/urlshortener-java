package com.urlshortener.service;

public interface ClickMetricsService {
    
    /**
     * Records a click as a metric in Prometheus
     *
     * @param shortCode the short code of the URL
     * @param country   the country of the visitor
     * @param browser   the browser used
     * @param platform  the platform used
     */
    void recordClick(String shortCode, String country, String browser, String platform);
} 