package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured outfit selection returned by the AI. Every {@code wardrobeItemId}
 * must be re-validated by Spring Boot against the user's actual wardrobe.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutfitSelectionResponse {

    private List<OutfitSelectionItem> items = new ArrayList<>();
    private String reason;
    private String hairstyle;

    public OutfitSelectionResponse() {
    }

    public OutfitSelectionResponse(List<OutfitSelectionItem> items, String reason, String hairstyle) {
        this.items = items;
        this.reason = reason;
        this.hairstyle = hairstyle;
    }

    public List<OutfitSelectionItem> getItems() {
        return items;
    }

    public void setItems(List<OutfitSelectionItem> items) {
        this.items = items;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHairstyle() {
        return hairstyle;
    }

    public void setHairstyle(String hairstyle) {
        this.hairstyle = hairstyle;
    }
}
