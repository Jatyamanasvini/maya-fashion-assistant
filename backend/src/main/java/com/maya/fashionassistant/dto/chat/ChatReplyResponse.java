package com.maya.fashionassistant.dto.chat;

import com.maya.fashionassistant.dto.outfit.OutfitResponse;
import com.maya.fashionassistant.dto.weather.WeatherInfo;

public class ChatReplyResponse {

    private String reply;
    private OutfitResponse outfit;
    private WeatherInfo weather;

    public ChatReplyResponse() {
    }

    public ChatReplyResponse(String reply, OutfitResponse outfit, WeatherInfo weather) {
        this.reply = reply;
        this.outfit = outfit;
        this.weather = weather;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public OutfitResponse getOutfit() {
        return outfit;
    }

    public void setOutfit(OutfitResponse outfit) {
        this.outfit = outfit;
    }

    public WeatherInfo getWeather() {
        return weather;
    }

    public void setWeather(WeatherInfo weather) {
        this.weather = weather;
    }
}
