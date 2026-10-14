package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single "item to buy" idea from the AI (no image yet — the image is resolved
 * separately via an external image search).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShoppingIdea {

    private String category;
    private String title;
    private String reason;
    private String query;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
