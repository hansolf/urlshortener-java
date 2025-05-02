package com.urlshortener.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

@Component
public class UserAgentParser {

    private static final Logger logger = Logger.getLogger(UserAgentParser.class.getName());
    private static final Pattern BROWSER_PATTERN = Pattern.compile(
        "(Chrome|Firefox|Safari|Edge|MSIE|Trident|Opera|OPR|SamsungBrowser|UCBrowser|YaBrowser|Brave)[/\\s](\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLATFORM_PATTERN = Pattern.compile(
        "(Windows|Mac|Macintosh|Linux|Android|iPhone|iPad|iOS|iPod|Unix|BSD|CrOS)", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Sample country codes with weighted distribution
    private static final String[] WEIGHTED_COUNTRIES = {
        "US", "US", "US", "US", "US", "US", "US", "US", "US", "US", // 10 US entries (33%)
        "GB", "GB", "GB", "CA", "CA", "CA", "DE", "DE", "DE", "FR", "FR", "FR", // 9 entries (30%)
        "JP", "JP", "CN", "CN", "RU", "RU", "BR", "IN", "IN", // 9 entries (30%)
        "AU", "MX", "ES", "IT", "NL", "SE", "KR", "ZA", "UA", "PL", "TR" // 11 entries (less common)
    };
    private static final Random RANDOM = new Random();
    
    // Map of country codes to names for display
    private static final Map<String, String> COUNTRY_NAMES = new HashMap<>();
    
    static {
        COUNTRY_NAMES.put("US", "United States");
        COUNTRY_NAMES.put("GB", "United Kingdom");
        COUNTRY_NAMES.put("CA", "Canada");
        COUNTRY_NAMES.put("DE", "Germany");
        COUNTRY_NAMES.put("FR", "France");
        COUNTRY_NAMES.put("JP", "Japan");
        COUNTRY_NAMES.put("CN", "China");
        COUNTRY_NAMES.put("RU", "Russia");
        COUNTRY_NAMES.put("BR", "Brazil");
        COUNTRY_NAMES.put("IN", "India");
        COUNTRY_NAMES.put("AU", "Australia");
        COUNTRY_NAMES.put("MX", "Mexico");
        COUNTRY_NAMES.put("ES", "Spain");
        COUNTRY_NAMES.put("IT", "Italy");
        COUNTRY_NAMES.put("NL", "Netherlands");
        COUNTRY_NAMES.put("SE", "Sweden");
        COUNTRY_NAMES.put("KR", "South Korea");
        COUNTRY_NAMES.put("ZA", "South Africa");
        COUNTRY_NAMES.put("UA", "Ukraine");
        COUNTRY_NAMES.put("PL", "Poland");
        COUNTRY_NAMES.put("TR", "Turkey");
        COUNTRY_NAMES.put("ZZ", "Unknown");
    }

    /**
     * Parses the user agent string to extract browser, platform, and other information
     *
     * @param userAgent The user agent string from the HTTP request
     * @param ipAddress The IP address of the request
     * @return A map containing browser, platform, and other information
     */
    public Map<String, String> parseUserAgent(String userAgent, String ipAddress) {
        Map<String, String> result = new HashMap<>();
        try {
            System.out.println("UserAgentParser input: " + userAgent);
            // Определение браузера
            String browser = extractBrowser(userAgent);
            String platform = extractPlatform(userAgent);
            String countryCode = "ZZ";
            String cityName = "Unknown";

            // Если локальный IP — подставляем случайные значения для dev
            if (ipAddress != null && (ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost"))) {
                // Случайная страна
                String[] countries = {"US", "GB", "CA", "DE", "FR", "RU", "JP", "CN", "BR", "IN"};
                countryCode = countries[RANDOM.nextInt(countries.length)];
                // Случайный браузер
                String[] browsers = {"Chrome", "Firefox", "Safari", "Edge", "Opera"};
                browser = browsers[RANDOM.nextInt(browsers.length)];
                // Случайная платформа
                String[] platforms = {"Windows", "macOS", "Linux", "Android", "iOS"};
                platform = platforms[RANDOM.nextInt(platforms.length)];
                cityName = generateSampleCity(countryCode);
            } else if (ipAddress != null && !ipAddress.equals("127.0.0.1") && !ipAddress.equals("localhost")) {
                try {
                    URL url = new URL("http://ip-api.com/json/" + ipAddress + "?fields=status,countryCode,city");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(1000);
                    con.setReadTimeout(1000);
                    int status = con.getResponseCode();
                    if (status == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            content.append(line);
                        }
                        in.close();
                        JSONObject obj = new JSONObject(content.toString());
                        if ("success".equals(obj.optString("status"))) {
                            countryCode = obj.optString("countryCode", "ZZ");
                            cityName = obj.optString("city", "Unknown");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("GeoIP lookup failed: " + e.getMessage());
                }
            }
            result.put("browser", browser);
            result.put("platform", platform);
            result.put("country", countryCode);
            result.put("city", cityName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing user agent: " + userAgent, e);
            setDefaultValues(result);
        }
        System.out.println("Parsed: " + result);
        return result;
    }
    
    /**
     * Extract browser information from user agent
     */
    private String extractBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Edg/")) return "Edge";
        if (userAgent.contains("OPR") || userAgent.contains("Opera")) return "Opera";
        if (userAgent.contains("YaBrowser")) return "Yandex Browser";
        if (userAgent.contains("SamsungBrowser")) return "Samsung Internet";
        if (userAgent.contains("UCBrowser")) return "UC Browser";
        if (userAgent.contains("Brave")) return "Brave";
        if (userAgent.contains("Vivaldi")) return "Vivaldi";
        if (userAgent.contains("Maxthon")) return "Maxthon";
        if (userAgent.contains("PaleMoon")) return "Pale Moon";
        if (userAgent.contains("Waterfox")) return "Waterfox";
        if (userAgent.contains("Coc Coc")) return "Coc Coc";
        if (userAgent.contains("SeaMonkey")) return "SeaMonkey";
        if (userAgent.contains("QQBrowser")) return "QQBrowser";
        if (userAgent.contains("Sogou")) return "Sogou";
        if (userAgent.contains("Sleipnir")) return "Sleipnir";
        if (userAgent.contains("Midori")) return "Midori";
        if (userAgent.contains("Dolphin")) return "Dolphin";
        if (userAgent.contains("Puffin")) return "Puffin";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("FxiOS")) return "Firefox iOS";
        if (userAgent.contains("Waterfox")) return "Waterfox";
        if (userAgent.contains("Opera GX")) return "Opera GX";
        if (userAgent.contains("Epic")) return "Epic";
        if (userAgent.contains("SRWare Iron")) return "SRWare Iron";
        if (userAgent.contains("CentBrowser")) return "Cent Browser";
        if (userAgent.contains("Comodo_Dragon")) return "Comodo Dragon";
        if (userAgent.contains("Torch")) return "Torch";
        if (userAgent.contains("Slimjet")) return "Slimjet";
        if (userAgent.contains("Lunascape")) return "Lunascape";
        if (userAgent.contains("Avant")) return "Avant";
        if (userAgent.contains("Otter")) return "Otter";
        if (userAgent.contains("Falkon")) return "Falkon";
        if (userAgent.contains("IceCat")) return "IceCat";
        if (userAgent.contains("Iceweasel")) return "Iceweasel";
        if (userAgent.contains("K-Meleon")) return "K-Meleon";
        if (userAgent.contains("Basilisk")) return "Basilisk";
        if (userAgent.contains("QuteBrowser")) return "QuteBrowser";
        if (userAgent.contains("Dooble")) return "Dooble";
        if (userAgent.contains("Polarity")) return "Polarity";
        if (userAgent.contains("Sputnik")) return "Sputnik";
        if (userAgent.contains("360Browser")) return "360 Browser";
        if (userAgent.contains("Baidu")) return "Baidu";
        if (userAgent.contains("Liebao")) return "Liebao";
        if (userAgent.contains("Smooz")) return "Smooz";
        if (userAgent.contains("Kiwi")) return "Kiwi";
        if (userAgent.contains("Phoenix")) return "Phoenix";
        if (userAgent.contains("BlackBerry")) return "BlackBerry";
        if (userAgent.contains("Silk")) return "Silk";
        if (userAgent.contains("MicroMessenger")) return "WeChat";
        if (userAgent.contains("MiuiBrowser")) return "Miui Browser";
        if (userAgent.contains("HuaweiBrowser")) return "Huawei Browser";
        if (userAgent.contains("Phoenix Browser")) return "Phoenix Browser";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) return "Internet Explorer";
        if (userAgent.toLowerCase().contains("bot") || userAgent.toLowerCase().contains("spider") || userAgent.toLowerCase().contains("crawler")) return "Bot";
        return "Other";
    }
    
    /**
     * Extract platform information from user agent
     */
    private String extractPlatform(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        
        Matcher platformMatcher = PLATFORM_PATTERN.matcher(userAgent);
        if (platformMatcher.find()) {
            String platform = platformMatcher.group(1);
            
            // Normalize platform names
            if ("Macintosh".equalsIgnoreCase(platform) || "Mac".equalsIgnoreCase(platform)) {
                return "macOS";
            } else if ("iPhone".equalsIgnoreCase(platform) || "iPad".equalsIgnoreCase(platform) || 
                       "iPod".equalsIgnoreCase(platform) || "iOS".equalsIgnoreCase(platform)) {
                return "iOS";
            } else if ("Windows".equalsIgnoreCase(platform)) {
                return "Windows";
            } else if ("Android".equalsIgnoreCase(platform)) {
                return "Android";
            } else if ("Linux".equalsIgnoreCase(platform)) {
                return "Linux";
            } else if ("CrOS".equalsIgnoreCase(platform)) {
                return "Chrome OS";
            }
            
            return platform;
        }
        
        // Mobile detection
        if (userAgent.contains("Mobile") || userAgent.contains("mobile")) {
            if (userAgent.contains("Android")) {
                return "Android";
            } else if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) {
                return "iOS";
            }
            return "Mobile";
        }
        
        return "Other";
    }
    
    /**
     * Gets the country name from country code
     * 
     * @param countryCode The two-letter country code
     * @return The full country name
     */
    public static String getCountryName(String countryCode) {
        return COUNTRY_NAMES.getOrDefault(countryCode, "Unknown");
    }
    
    /**
     * Sets default values for the result map
     * 
     * @param result The map to set default values on
     */
    private void setDefaultValues(Map<String, String> result) {
        result.put("browser", "Unknown");
        result.put("platform", "Unknown");
        result.put("country", "ZZ");
        result.put("city", "Unknown");
    }
    
    /**
     * Generates a sample city name for demonstration
     * 
     * @param countryCode The country code
     * @return A sample city name
     */
    private String generateSampleCity(String countryCode) {
        switch (countryCode) {
            case "US": 
                String[] usCities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", "Dallas"};
                return usCities[RANDOM.nextInt(usCities.length)];
            case "GB": 
                String[] gbCities = {"London", "Manchester", "Birmingham", "Glasgow", "Liverpool", "Edinburgh"};
                return gbCities[RANDOM.nextInt(gbCities.length)];
            case "CA": 
                String[] caCities = {"Toronto", "Montreal", "Vancouver", "Calgary", "Ottawa"};
                return caCities[RANDOM.nextInt(caCities.length)];
            case "DE": 
                String[] deCities = {"Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt"};
                return deCities[RANDOM.nextInt(deCities.length)];
            case "FR": 
                String[] frCities = {"Paris", "Marseille", "Lyon", "Toulouse", "Nice"};
                return frCities[RANDOM.nextInt(frCities.length)];
            case "JP": 
                String[] jpCities = {"Tokyo", "Osaka", "Yokohama", "Nagoya", "Sapporo"};
                return jpCities[RANDOM.nextInt(jpCities.length)];
            case "CN": 
                String[] cnCities = {"Beijing", "Shanghai", "Guangzhou", "Shenzhen", "Chengdu"};
                return cnCities[RANDOM.nextInt(cnCities.length)];
            case "RU": 
                String[] ruCities = {"Moscow", "Saint Petersburg", "Novosibirsk", "Yekaterinburg", "Kazan"};
                return ruCities[RANDOM.nextInt(ruCities.length)];
            case "BR": 
                String[] brCities = {"Rio de Janeiro", "São Paulo", "Salvador", "Brasília", "Fortaleza"};
                return brCities[RANDOM.nextInt(brCities.length)];
            case "IN": 
                String[] inCities = {"Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai"};
                return inCities[RANDOM.nextInt(inCities.length)];
            case "AU": 
                String[] auCities = {"Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide"};
                return auCities[RANDOM.nextInt(auCities.length)];
            case "MX": 
                String[] mxCities = {"Mexico City", "Guadalajara", "Monterrey", "Puebla", "Tijuana"};
                return mxCities[RANDOM.nextInt(mxCities.length)];
            case "ES": 
                String[] esCities = {"Madrid", "Barcelona", "Valencia", "Seville", "Zaragoza"};
                return esCities[RANDOM.nextInt(esCities.length)];
            default: 
                return "Unknown";
        }
    }
} 