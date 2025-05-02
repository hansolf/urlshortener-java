package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.UrlDto;
import com.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicUrlController {

    @Autowired
    private UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<UrlDto>> createShortUrl(@Valid @RequestBody UrlDto urlDto) {
        // For public URLs, we don't associate with a user
        UrlDto createdUrl = urlService.shortenUrl(urlDto.getOriginalUrl(), null);
        return ResponseEntity.ok(ApiResponse.success("URL shortened successfully", createdUrl));
    }

    @GetMapping("/url/{shortCode}")
    public ResponseEntity<ApiResponse<UrlDto>> getUrlByShortCode(@PathVariable String shortCode) {
        UrlDto url = urlService.getUrlByShortCode(shortCode);
        return ResponseEntity.ok(ApiResponse.success(url));
    }
} 