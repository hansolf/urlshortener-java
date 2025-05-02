package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsDto;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/url/{urlId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AnalyticsDto>> getAnalyticsForUrl(@PathVariable Long urlId) {
        AnalyticsDto analytics = analyticsService.getAnalyticsForUrl(urlId);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
} 