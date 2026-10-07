package com.maya.fashionassistant.weather;

import com.maya.fashionassistant.dto.weather.WeatherInfo;

/**
 * Abstraction over a weather provider. Implementations translate provider-specific
 * responses into the normalized {@link WeatherInfo} representation.
 */
public interface WeatherClient {

    /**
     * Fetches current weather for the given coordinates.
     *
     * @param latitude  latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @return normalized weather snapshot, or {@code null} when unavailable
     */
    WeatherInfo getCurrentWeather(double latitude, double longitude);
}
