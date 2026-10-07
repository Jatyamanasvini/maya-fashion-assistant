package com.maya.fashionassistant.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.weather.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Open-Meteo client. The free tier requires no API key and exposes
 * {@code GET /v1/forecast} with a {@code current} block.
 */
@Component
public class OpenMeteoWeatherClient implements WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoWeatherClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoWeatherClient(
            @Value("${weather.base-url:https://api.open-meteo.com/v1}") String baseUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public WeatherInfo getCurrentWeather(double latitude, double longitude) {
        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current",
                                    "temperature_2m,weather_code,wind_speed_10m,precipitation")
                            .queryParam("temperature_unit", "celsius")
                            .build())
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            JsonNode current = objectMapper.readTree(responseBody).path("current");
            if (current.isMissingNode()) {
                return null;
            }

            Double temperature = current.hasNonNull("temperature_2m")
                    ? current.get("temperature_2m").asDouble() : null;
            int weatherCode = current.hasNonNull("weather_code")
                    ? current.get("weather_code").asInt() : 0;
            Double windSpeed = current.hasNonNull("wind_speed_10m")
                    ? current.get("wind_speed_10m").asDouble() : null;
            Double precipitation = current.hasNonNull("precipitation")
                    ? current.get("precipitation").asDouble() : null;

            return new WeatherInfo(temperature, describeCode(weatherCode), windSpeed, precipitation, null);
        } catch (Exception e) {
            log.warn("Open-Meteo weather lookup failed: {}", e.getMessage());
            return null;
        }
    }

    private String describeCode(int code) {
        if (code == 0) return "Clear";
        if (code <= 3) return "Partly cloudy";
        if (code <= 48) return "Fog";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Rain showers";
        if (code <= 86) return "Snow showers";
        if (code <= 99) return "Thunderstorm";
        return "Unknown";
    }
}
