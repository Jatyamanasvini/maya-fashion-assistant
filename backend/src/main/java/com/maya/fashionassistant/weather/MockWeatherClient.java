package com.maya.fashionassistant.weather;

import com.maya.fashionassistant.dto.weather.WeatherInfo;
import org.springframework.stereotype.Component;

/**
 * Deterministic weather client for offline development and automated tests.
 */
@Component
public class MockWeatherClient implements WeatherClient {

    @Override
    public WeatherInfo getCurrentWeather(double latitude, double longitude) {
        return new WeatherInfo(22.0, "Clear", 8.0, 0.0, "Mock City");
    }
}
