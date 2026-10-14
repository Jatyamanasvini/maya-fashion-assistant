package com.maya.fashionassistant.dto.shopping;

/**
 * A "shop this" style idea shown when the user's wardrobe cannot fulfil a request.
 * These are explicitly NOT owned items — they are external suggestions.
 */
public class ShoppingSuggestion {

    private String category;
    private String title;
    private String reason;
    private String imageUrl;
    private String sourceUrl;

    public ShoppingSuggestion() {
    }

    public ShoppingSuggestion(String category, String title, String reason, String imageUrl, String sourceUrl) {
        this.category = category;
        this.title = title;
        this.reason = reason;
        this.imageUrl = imageUrl;
        this.sourceUrl = sourceUrl;
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
