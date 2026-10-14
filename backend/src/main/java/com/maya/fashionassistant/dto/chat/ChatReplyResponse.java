package com.maya.fashionassistant.dto.chat;

import com.maya.fashionassistant.dto.outfit.OutfitResponse;
import com.maya.fashionassistant.dto.shopping.ShoppingSuggestion;
import com.maya.fashionassistant.dto.weather.WeatherInfo;

import java.util.List;

public class ChatReplyResponse {

    private String reply;
    private OutfitResponse outfit;
    private WeatherInfo weather;
    private List<ShoppingSuggestion> suggestions;

    public ChatReplyResponse() {
    }

    public ChatReplyResponse(String reply, OutfitResponse outfit, WeatherInfo weather) {
        this.reply = reply;
        this.outfit = outfit;
        this.weather = weather;
    }

    public ChatReplyResponse(String reply, OutfitResponse outfit, WeatherInfo weather,
                             List<ShoppingSuggestion> suggestions) {
        this.reply = reply;
        this.outfit = outfit;
        this.weather = weather;
        this.suggestions = suggestions;
    }

    public List<ShoppingSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<ShoppingSuggestion> suggestions) {
        this.suggestions = suggestions;
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
