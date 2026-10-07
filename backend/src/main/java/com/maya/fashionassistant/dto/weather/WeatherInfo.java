package com.maya.fashionassistant.dto.weather;

/**
 * Normalized weather snapshot used to inform outfit recommendations.
 * Deliberately coarse: only aggregate conditions, no precise coordinates persisted.
 */
public class WeatherInfo {

    private Double temperature;      // degrees Celsius
    private String condition;        // human-readable condition e.g. "Clear", "Rain"
    private Double windSpeed;        // km/h
    private Double precipitation;    // mm
    private String city;

    public WeatherInfo() {
    }

    public WeatherInfo(Double temperature, String condition, Double windSpeed, Double precipitation, String city) {
        this.temperature = temperature;
        this.condition = condition;
        this.windSpeed = windSpeed;
        this.precipitation = precipitation;
        this.city = city;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(Double precipitation) {
        this.precipitation = precipitation;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
