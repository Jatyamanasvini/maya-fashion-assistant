package com.maya.fashionassistant.config;

import com.maya.fashionassistant.weather.MockWeatherClient;
import com.maya.fashionassistant.weather.OpenMeteoWeatherClient;
import com.maya.fashionassistant.weather.WeatherClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WeatherConfig {

    private static final Logger log = LoggerFactory.getLogger(WeatherConfig.class);

    @Value("${weather.provider:OPEN_METEO}")
    private String weatherProvider;

    @Bean
    @Primary
    public WeatherClient activeWeatherClient(
            OpenMeteoWeatherClient openMeteoWeatherClient,
            MockWeatherClient mockWeatherClient) {
        if ("MOCK".equalsIgnoreCase(weatherProvider)) {
            log.info("Configured active weather provider: Mock");
            return mockWeatherClient;
        }
        log.info("Configured active weather provider: Open-Meteo");
        return openMeteoWeatherClient;
    }
}
