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

            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            String userAgent = request.getHeader("User-Agent");
            String referrer = request.getHeader("Referer");

            System.out.println("Redirect: " + shortCode + " IP: " + ipAddress + " UA: " + userAgent + " Ref: " + referrer);

            urlService.recordClick(shortCode, ipAddress, userAgent, referrer);
            
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(originalUrl);
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        } catch (Exception e) {
            System.err.println("Error redirecting: " + e.getMessage());
            e.printStackTrace();

            RedirectView redirectView = new RedirectView();
            redirectView.setUrl("/not-found");
            return redirectView;
        }
    }
} 