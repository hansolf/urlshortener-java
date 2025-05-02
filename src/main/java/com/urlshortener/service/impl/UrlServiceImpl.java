package com.urlshortener.service.impl;

import com.google.common.hash.Hashing;
import com.urlshortener.dto.UrlDto;
import com.urlshortener.dto.analytics.ClickEventDto;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.model.Analytics;
import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.AnalyticsRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.ClickMetricsService;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.UserAgentParser;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.urlshortener.config.RabbitMQConfig.ANALYTICS_EXCHANGE;
import static com.urlshortener.config.RabbitMQConfig.ANALYTICS_ROUTING_KEY;

@Service
public class UrlServiceImpl implements UrlService {

    private static final Logger logger = Logger.getLogger(UrlServiceImpl.class.getName());

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserAgentParser userAgentParser;

    @Autowired
    private ClickMetricsService clickMetricsService;

    @Value("${url-shortener.base-url}")
    private String baseUrl;

    @Value("${url-shortener.cache.ttl-hours}")
    private int cacheTtlHours;

    private static final String URL_KEY_PREFIX = "url:";
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    @Override
    @Transactional
    public UrlDto shortenUrl(String originalUrl, User user) {
        // Validate URL
        if (!URL_VALIDATOR.isValid(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL: " + originalUrl);
        }

        // Generate short code
        String shortCode = generateShortCode(originalUrl);

        // Check if short code already exists
        while (urlRepository.existsByShortCode(shortCode)) {
            shortCode = generateShortCode(originalUrl + System.nanoTime());
        }

        // Create new URL entity
        Url url = Url.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .user(user)
                .build();

        // Save to database
        url = urlRepository.save(url);

        // Save to Redis cache
        String cacheKey = URL_KEY_PREFIX + shortCode;
        redisTemplate.opsForValue().set(cacheKey, originalUrl, cacheTtlHours, TimeUnit.HOURS);

        return buildUrlDto(url, 0L);
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        // Try to get from cache first
        String cacheKey = URL_KEY_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            return cachedUrl;
        }

        // If not in cache, get from database
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found for code: " + shortCode));

        // Save to cache for future requests
        redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), cacheTtlHours, TimeUnit.HOURS);

        return url.getOriginalUrl();
    }

    @Override
    public UrlDto getUrlByShortCode(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found for code: " + shortCode));

        Long clickCount = analyticsRepository.countByUrl(url);
        return buildUrlDto(url, clickCount);
    }

    @Override
    public UrlDto getUrlById(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));

        Long clickCount = analyticsRepository.countByUrl(url);
        return buildUrlDto(url, clickCount);
    }

    @Override
    public Page<UrlDto> getUrlsByUser(User user, Pageable pageable) {
        Page<Url> urls = urlRepository.findByUser(user, pageable);
        return urls.map(url -> {
            Long clickCount = analyticsRepository.countByUrl(url);
            return buildUrlDto(url, clickCount);
        });
    }

    @Override
    @Transactional
    public void deleteUrl(Long id, User user) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));

        // Разрешить удалять только владельцу ссылки (без проверки ролей)
        if (url.getUser() == null || !Objects.equals(url.getUser().getId(), user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this URL");
        }

        // Удаляем все связанные записи аналитики
        analyticsRepository.deleteAll(analyticsRepository.findByUrl(url));

        // Delete from Redis cache
        String cacheKey = URL_KEY_PREFIX + url.getShortCode();
        redisTemplate.delete(cacheKey);

        // Delete from database
        urlRepository.delete(url);
    }

    @Override
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referrer) {
        // Find URL by short code
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with short code: " + shortCode));

        try {
            logger.info("Recording click for shortCode: " + shortCode);
            logger.info("IP address: " + ipAddress);
            logger.info("User agent: " + userAgent);
            logger.info("Referrer: " + (referrer != null ? referrer : "none"));
            
            // Create and save analytics entry directly to ensure it's always recorded
            Map<String, String> parsedUserAgent = userAgentParser.parseUserAgent(userAgent, ipAddress);
            String browser = parsedUserAgent.get("browser");
            String platform = parsedUserAgent.get("platform");
            String country = parsedUserAgent.get("country");
            
            logger.info("Parsed user agent - Browser: " + browser + ", Platform: " + platform + ", Country: " + UserAgentParser.getCountryName(country));
            
            Analytics analytics = Analytics.builder()
                    .url(url)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .browser(browser)
                    .platform(platform)
                    .country(country)
                    .city(parsedUserAgent.get("city"))
                    .timestamp(LocalDateTime.now())
                    .build();
            analyticsRepository.save(analytics);
            logger.info("Analytics saved to database with ID: " + analytics.getId());
            
            // Record metrics for Prometheus
            clickMetricsService.recordClick(shortCode, country, browser, platform);
            logger.info("Click metrics sent to Prometheus");
            
            // Also send to message queue for any additional asynchronous processing
            ClickEventDto clickEventDto = ClickEventDto.builder()
                    .urlId(url.getId())
                    .shortCode(shortCode)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .browser(browser)
                    .platform(platform)
                    .country(country)
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(ANALYTICS_EXCHANGE, ANALYTICS_ROUTING_KEY, clickEventDto);
            logger.info("Click event sent to RabbitMQ for async processing");
        } catch (Exception e) {
            // Log the error but don't throw it to avoid affecting the redirect
            logger.warning("Error recording click: " + e.getMessage());
        }
    }

    private String generateShortCode(String originalUrl) {
        LocalDateTime now = LocalDateTime.now();
        String input = originalUrl + now.toString();
        return Hashing.murmur3_32_fixed()
                .hashString(input, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 7);
    }

    private UrlDto buildUrlDto(Url url, Long clickCount) {
        // Always fetch the latest click count if zero is passed
        if (clickCount == 0L) {
            clickCount = analyticsRepository.countByUrl(url);
        }

        return UrlDto.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "s/" + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expireAt(url.getExpireAt())
                .clickCount(clickCount)
                .build();
    }
} 