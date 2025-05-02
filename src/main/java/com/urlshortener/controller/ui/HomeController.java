package com.urlshortener.controller.ui;

import com.urlshortener.dto.UrlDto;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private UrlService urlService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("urlDto", new UrlDto());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            User user = getCurrentUser();
            Page<UrlDto> urls = urlService.getUrlsByUser(user, PageRequest.of(page, 10));
            model.addAttribute("urls", urls);
        }
        
        return "home";
    }

    @PostMapping("/")
    public String shortenUrl(@ModelAttribute UrlDto urlDto, Model model) {
        try {
            User user = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                user = getCurrentUser();
            }
            
            UrlDto shortUrl = urlService.shortenUrl(urlDto.getOriginalUrl(), user);
            model.addAttribute("shortUrl", shortUrl);
            model.addAttribute("urlDto", new UrlDto());
            
            if (user != null) {
                Page<UrlDto> urls = urlService.getUrlsByUser(user, PageRequest.of(0, 10));
                model.addAttribute("urls", urls);
            }
            
            return "home";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid URL: " + e.getMessage());
            return "home";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/not-found")
    public String notFound() {
        return "not-found";
    }

    @PostMapping("/urls/{id}")
    public String deleteUrl(@PathVariable Long id, @RequestParam(value = "_method", required = false) String method, Model model) {
        if ("DELETE".equalsIgnoreCase(method)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                User user = getCurrentUser();
                urlService.deleteUrl(id, user);
            }
        }
        return "redirect:/";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
} 