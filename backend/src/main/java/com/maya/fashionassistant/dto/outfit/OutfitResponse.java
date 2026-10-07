package com.maya.fashionassistant.dto.outfit;

import java.time.LocalDateTime;
import java.util.List;

public class OutfitResponse {

    private Long id;
    private String hairstyle;
    private String reason;
    private String generatedImageUrl;
    private LocalDateTime createdAt;
    private List<OutfitItemResponse> items;

    public OutfitResponse() {
    }

    public OutfitResponse(Long id, String hairstyle, String reason, String generatedImageUrl,
                          LocalDateTime createdAt, List<OutfitItemResponse> items) {
        this.id = id;
        this.hairstyle = hairstyle;
        this.reason = reason;
        this.generatedImageUrl = generatedImageUrl;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHairstyle() {
        return hairstyle;
    }

    public void setHairstyle(String hairstyle) {
        this.hairstyle = hairstyle;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getGeneratedImageUrl() {
        return generatedImageUrl;
    }

    public void setGeneratedImageUrl(String generatedImageUrl) {
        this.generatedImageUrl = generatedImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OutfitItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OutfitItemResponse> items) {
        this.items = items;
    }
}
