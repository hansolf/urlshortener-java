package com.urlshortener.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventDto {
    private Long urlId;
    private String shortCode;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private String browser;
    private String platform;
    private String country;
    private LocalDateTime timestamp;
} 