package com.urlshortener.controller.ui;

import com.urlshortener.dto.AnalyticsDto;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Controller
@RequestMapping("/analytics")
@PreAuthorize("isAuthenticated()")
public class AnalyticsViewController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/url/{urlId}")
    public String getAnalyticsForUrl(@PathVariable Long urlId, Model model) {
        // Check if URL belongs to current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        
        User currentUser = getCurrentUser();
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));

        // If URL has an owner, make sure it's the current user
        if (url.getUser() != null && !Objects.equals(url.getUser().getId(), currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view analytics for this URL");
        }

        AnalyticsDto analytics = analyticsService.getAnalyticsForUrl(url);
        model.addAttribute("analytics", analytics);
        
        return "analytics";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
} 