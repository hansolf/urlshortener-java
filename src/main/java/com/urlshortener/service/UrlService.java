package com.urlshortener.service;

import com.urlshortener.dto.UrlDto;
import com.urlshortener.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UrlService {
    UrlDto shortenUrl(String originalUrl, User user);
    String getOriginalUrl(String shortCode);
    UrlDto getUrlByShortCode(String shortCode);
    Page<UrlDto> getUrlsByUser(User user, Pageable pageable);
    void deleteUrl(Long id, User user);
    UrlDto getUrlById(Long id);
    void recordClick(String shortCode, String ipAddress, String userAgent, String referrer);
} 