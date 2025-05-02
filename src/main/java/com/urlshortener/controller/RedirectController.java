package com.urlshortener.controller;

import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class RedirectController {

    @Autowired
    private UrlService urlService;

    @GetMapping("/s/{shortCode}")
    public RedirectView redirectToOriginalUrl(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            String originalUrl = urlService.getOriginalUrl(shortCode);
            
            // Record analytics asynchronously
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            String referrer = request.getHeader("Referer");
            
            // Log the click data
            System.out.println("Redirect: " + shortCode + " IP: " + ipAddress + " UA: " + userAgent + " Ref: " + referrer);
            
            // Record the click
            urlService.recordClick(shortCode, ipAddress, userAgent, referrer);
            
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(originalUrl);
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error redirecting: " + e.getMessage());
            e.printStackTrace();
            
            // If the URL is not found, redirect to the home page or an error page
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl("/not-found");
            return redirectView;
        }
    }
} 