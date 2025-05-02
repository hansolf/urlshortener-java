package com.urlshortener.service.impl;

import com.urlshortener.service.ClickMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClickMetricsServiceImpl implements ClickMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    @Autowired
    public ClickMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Records a click as a metric in Prometheus
     *
     * @param shortCode the short code of the URL
     * @param country   the country of the visitor
     * @param browser   the browser used
     * @param platform  the platform used
     */
    @Override
    public void recordClick(String shortCode, String country, String browser, String platform) {
        meterRegistry.counter("url_clicks", 
            "shortCode", shortCode,
            "country", country != null ? country : "unknown",
            "browser", browser != null ? browser : "unknown",
            "platform", platform != null ? platform : "unknown"
        ).increment();
        
        // Also record separate metrics for easier querying
        meterRegistry.counter("url_clicks_by_country", "country", country != null ? country : "unknown").increment();
        meterRegistry.counter("url_clicks_by_browser", "browser", browser != null ? browser : "unknown").increment();
        meterRegistry.counter("url_clicks_by_platform", "platform", platform != null ? platform : "unknown").increment();
        meterRegistry.counter("url_clicks_by_shortcode", "shortCode", shortCode).increment();
    }
} 