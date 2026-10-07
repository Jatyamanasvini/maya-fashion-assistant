package com.maya.fashionassistant.service;

import com.maya.fashionassistant.dto.weather.WeatherInfo;
import com.maya.fashionassistant.weather.WeatherClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orchestrates weather retrieval for outfit context. Location is optional:
 * when no coordinates are supplied, falls back to the configured default city,
 * and returns {@code null} if the provider is unavailable rather than failing.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherClient weatherClient;
    private final String defaultCity;
    private final double defaultLat;
    private final double defaultLon;

    public WeatherService(
            WeatherClient weatherClient,
            @Value("${weather.default-city:New York}") String defaultCity,
            @Value("${weather.default-lat:40.7128}") double defaultLat,
            @Value("${weather.default-lon:-74.0060}") double defaultLon) {
        this.weatherClient = weatherClient;
        this.defaultCity = defaultCity;
        this.defaultLat = defaultLat;
        this.defaultLon = defaultLon;
    }

    /**
     * Returns current weather for the supplied coordinates, or the configured
     * default location when coordinates are absent.
     *
     * @return normalized weather, or {@code null} when unavailable
     */
    public WeatherInfo getWeather(Double latitude, Double longitude) {
        double lat = latitude != null ? latitude : defaultLat;
        double lon = longitude != null ? longitude : defaultLon;

        try {
            WeatherInfo weather = weatherClient.getCurrentWeather(lat, lon);
            if (weather != null && weather.getCity() == null && latitude == null) {
                weather.setCity(defaultCity);
            }
            return weather;
        } catch (Exception e) {
            log.warn("Weather lookup failed: {}", e.getMessage());
            return null;
        }
    }
}
