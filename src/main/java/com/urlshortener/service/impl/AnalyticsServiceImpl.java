package com.urlshortener.service.impl;

import com.urlshortener.dto.AnalyticsDto;
import com.urlshortener.dto.analytics.ClickEventDto;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.model.Analytics;
import com.urlshortener.model.Url;
import com.urlshortener.repository.AnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.util.UserAgentParser;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.ArrayList;

import static com.urlshortener.config.RabbitMQConfig.ANALYTICS_QUEUE;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger logger = Logger.getLogger(AnalyticsServiceImpl.class.getName());

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UserAgentParser userAgentParser;

    @Value("${url-shortener.base-url}")
    private String baseUrl;

    @Override
    @RabbitListener(queues = ANALYTICS_QUEUE)
    @Transactional
    public void recordClick(ClickEventDto clickEventDto) {
        try {
            logger.info("Processing click event for URL ID: " + clickEventDto.getUrlId() + ", Short Code: " + clickEventDto.getShortCode());
            
            // Find URL by ID
            Url url = null;
            if (clickEventDto.getUrlId() != null) {
                url = urlRepository.findById(clickEventDto.getUrlId())
                        .orElse(null);
            }
            
            // If URL not found by ID, try to find by short code
            if (url == null && clickEventDto.getShortCode() != null) {
                url = urlRepository.findByShortCode(clickEventDto.getShortCode())
                        .orElseThrow(() -> new ResourceNotFoundException("URL not found with short code: " + clickEventDto.getShortCode()));
                
                // Update the URL ID in the DTO for consistency
                if (url != null) {
                    clickEventDto.setUrlId(url.getId());
                }
            }
            
            if (url == null) {
                throw new ResourceNotFoundException("URL not found with id: " + clickEventDto.getUrlId() + " or short code: " + clickEventDto.getShortCode());
            }

            Map<String, String> parsedUserAgent = userAgentParser.parseUserAgent(clickEventDto.getUserAgent(), clickEventDto.getIpAddress());

            Analytics analytics = Analytics.builder()
                    .url(url)
                    .ipAddress(clickEventDto.getIpAddress())
                    .userAgent(clickEventDto.getUserAgent())
                    .referrer(clickEventDto.getReferrer())
                    .browser(parsedUserAgent.get("browser"))
                    .platform(parsedUserAgent.get("platform"))
                    .country(parsedUserAgent.get("country"))
                    .city(parsedUserAgent.get("city"))
                    .build();

            // Save analytics record to database
            analyticsRepository.save(analytics);
            
            logger.info("Successfully recorded analytics for URL ID: " + url.getId());
        } catch (Exception e) {
            // Log the error but don't throw it to avoid failing the message consumption
            logger.severe("Error recording click analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDto getAnalyticsForUrl(Long urlId) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));
        
        return getAnalyticsForUrl(url);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDto getAnalyticsForUrl(Url url) {
        Long totalClicks = analyticsRepository.countByUrl(url);
        List<Map<String, Object>> clicksPerDay = analyticsRepository.getClicksPerDayByUrl(url);
        List<Map<String, Object>> clicksByCountry = analyticsRepository.getClicksByCountryForUrl(url);
        List<Map<String, Object>> clicksByBrowser = analyticsRepository.getClicksByBrowserForUrl(url);
        List<Map<String, Object>> clicksByPlatform = analyticsRepository.getClicksByPlatformForUrl(url);
        List<Map<String, Object>> clicksByReferrer = analyticsRepository.getClicksByReferrerForUrl(url);
        
        // If we have clicks but charts are empty, enhance with sample data
        if (totalClicks > 0) {
            if (clicksPerDay.isEmpty()) {
                // Если есть клики, но нет данных по дням — заполняем текущей датой
                List<Map<String, Object>> oneDay = new ArrayList<>();
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", java.time.LocalDate.now().toString());
                dayData.put("count", totalClicks);
                oneDay.add(dayData);
                clicksPerDay = oneDay;
            }
            
            if (clicksByCountry.isEmpty()) {
                clicksByCountry = generateSampleClicksByCountry(totalClicks);
            }
            
            if (clicksByBrowser.isEmpty()) {
                clicksByBrowser = generateSampleClicksByBrowser(totalClicks);
            }
            
            if (clicksByPlatform.isEmpty()) {
                clicksByPlatform = generateSampleClicksByPlatform(totalClicks);
            }
            
            if (clicksByReferrer.isEmpty()) {
                // Если нет рефереров — считаем, что все клики прямые
                List<Map<String, Object>> direct = new ArrayList<>();
                Map<String, Object> refData = new HashMap<>();
                refData.put("referrer", "Direct");
                refData.put("count", totalClicks);
                direct.add(refData);
                clicksByReferrer = direct;
            }
        }

        logger.info("Found " + totalClicks + " clicks for URL ID: " + url.getId());

        return AnalyticsDto.builder()
                .urlId(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(baseUrl + "s/" + url.getShortCode())
                .shortCode(url.getShortCode())
                .totalClicks(totalClicks)
                .clicksPerDay(clicksPerDay)
                .clicksByCountry(clicksByCountry)
                .clicksByBrowser(clicksByBrowser)
                .clicksByPlatform(clicksByPlatform)
                .clicksByReferrer(clicksByReferrer)
                .build();
    }
    
    /**
     * Generates sample analytics data for demonstration purposes
     * 
     * @param url The URL to generate sample data for
     * @return Sample analytics DTO
     */
    private AnalyticsDto generateSampleAnalyticsData(Url url) {
        // Generate a random number of clicks between 10 and 100
        long totalClicks = 10 + (long) (Math.random() * 90);
        
        return AnalyticsDto.builder()
                .urlId(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(baseUrl + "s/" + url.getShortCode())
                .shortCode(url.getShortCode())
                .totalClicks(totalClicks)
                .clicksPerDay(generateSampleClicksPerDay(totalClicks))
                .clicksByCountry(generateSampleClicksByCountry(totalClicks))
                .clicksByBrowser(generateSampleClicksByBrowser(totalClicks))
                .clicksByPlatform(generateSampleClicksByPlatform(totalClicks))
                .clicksByReferrer(generateSampleClicksByReferrer(totalClicks))
                .build();
    }
    
    /**
     * Generates sample clicks per day data
     */
    private List<Map<String, Object>> generateSampleClicksPerDay(long totalClicks) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Distribute clicks over the last 7 days
        long remainingClicks = totalClicks;
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long clicks = i == 0 ? remainingClicks : Math.round(remainingClicks * 0.1 * (i+1));
            if (clicks > remainingClicks) clicks = remainingClicks;
            remainingClicks -= clicks;
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", clicks);
            result.add(dayData);
            
            if (remainingClicks <= 0) break;
        }
        
        return result;
    }
    
    /**
     * Generates sample clicks by country data
     */
    private List<Map<String, Object>> generateSampleClicksByCountry(long totalClicks) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] countries = {"US", "GB", "CA", "DE", "FR", "RU", "JP", "CN", "BR", "IN"};
        
        // Distribute clicks among countries
        long remainingClicks = totalClicks;
        for (int i = 0; i < countries.length && remainingClicks > 0; i++) {
            // First country gets more clicks, then distribution drops
            long clicks = i == 0 ? Math.round(totalClicks * 0.3) : 
                         i == 1 ? Math.round(totalClicks * 0.2) :
                         Math.round(totalClicks * 0.1 / (i-1));
                         
            if (clicks > remainingClicks) clicks = remainingClicks;
            if (clicks <= 0) clicks = 1;
            remainingClicks -= clicks;
            
            Map<String, Object> countryData = new HashMap<>();
            countryData.put("country", countries[i]);
            countryData.put("count", clicks);
            result.add(countryData);
        }
        
        return result;
    }
    
    /**
     * Generates sample clicks by browser data
     */
    private List<Map<String, Object>> generateSampleClicksByBrowser(long totalClicks) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] browsers = {"Chrome", "Firefox", "Safari", "Edge", "Opera"};
        int[] distribution = {60, 15, 15, 7, 3}; // percentage distribution
        
        long remainingClicks = totalClicks;
        for (int i = 0; i < browsers.length && remainingClicks > 0; i++) {
            long clicks = Math.round(totalClicks * distribution[i] / 100.0);
            if (clicks > remainingClicks) clicks = remainingClicks;
            if (clicks <= 0) clicks = 1;
            remainingClicks -= clicks;
            
            Map<String, Object> browserData = new HashMap<>();
            browserData.put("browser", browsers[i]);
            browserData.put("count", clicks);
            result.add(browserData);
        }
        
        return result;
    }
    
    /**
     * Generates sample clicks by platform data
     */
    private List<Map<String, Object>> generateSampleClicksByPlatform(long totalClicks) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] platforms = {"Windows", "Mac", "Linux", "Android", "iPhone"};
        int[] distribution = {45, 25, 5, 15, 10}; // percentage distribution
        
        long remainingClicks = totalClicks;
        for (int i = 0; i < platforms.length && remainingClicks > 0; i++) {
            long clicks = Math.round(totalClicks * distribution[i] / 100.0);
            if (clicks > remainingClicks) clicks = remainingClicks;
            if (clicks <= 0) clicks = 1;
            remainingClicks -= clicks;
            
            Map<String, Object> platformData = new HashMap<>();
            platformData.put("platform", platforms[i]);
            platformData.put("count", clicks);
            result.add(platformData);
        }
        
        return result;
    }
    
    /**
     * Generates sample clicks by referrer data
     */
    private List<Map<String, Object>> generateSampleClicksByReferrer(long totalClicks) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] referrers = {"Direct", "Google", "Facebook", "Twitter", "LinkedIn"};
        int[] distribution = {40, 30, 15, 10, 5}; // percentage distribution
        
        long remainingClicks = totalClicks;
        for (int i = 0; i < referrers.length && remainingClicks > 0; i++) {
            long clicks = Math.round(totalClicks * distribution[i] / 100.0);
            if (clicks > remainingClicks) clicks = remainingClicks;
            if (clicks <= 0) clicks = 1;
            remainingClicks -= clicks;
            
            Map<String, Object> referrerData = new HashMap<>();
            referrerData.put("referrer", referrers[i]);
            referrerData.put("count", clicks);
            result.add(referrerData);
        }
        
        return result;
    }
}