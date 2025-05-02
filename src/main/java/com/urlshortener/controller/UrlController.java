package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.UrlDto;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    @Autowired
    private UrlService urlService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UrlDto>> createShortUrl(@Valid @RequestBody UrlDto urlDto) {
        User user = getCurrentUser();
        UrlDto createdUrl = urlService.shortenUrl(urlDto.getOriginalUrl(), user);
        return ResponseEntity.ok(ApiResponse.success("URL shortened successfully", createdUrl));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<UrlDto>>> getMyUrls(Pageable pageable) {
        User user = getCurrentUser();
        Page<UrlDto> urls = urlService.getUrlsByUser(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(urls));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UrlDto>> getUrlById(@PathVariable Long id) {
        UrlDto url = urlService.getUrlById(id);
        return ResponseEntity.ok(ApiResponse.success(url));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable Long id) {
        User user = getCurrentUser();
        urlService.deleteUrl(id, user);
        return ResponseEntity.ok(ApiResponse.success("URL deleted successfully", null));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
} 