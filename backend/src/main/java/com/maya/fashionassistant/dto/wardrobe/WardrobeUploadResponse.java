package com.maya.fashionassistant.dto.wardrobe;

import java.time.LocalDateTime;
import java.util.List;

public class WardrobeUploadResponse {

    private Long sourceId;
    private String originalImagePath;
    private LocalDateTime uploadedAt;
    private List<WardrobeItemResponse> items;

    public WardrobeUploadResponse() {
    }

    public WardrobeUploadResponse(Long sourceId, String originalImagePath, LocalDateTime uploadedAt, List<WardrobeItemResponse> items) {
        this.sourceId = sourceId;
        this.originalImagePath = originalImagePath;
        this.uploadedAt = uploadedAt;
        this.items = items;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getOriginalImagePath() {
        return originalImagePath;
    }

    public void setOriginalImagePath(String originalImagePath) {
        this.originalImagePath = originalImagePath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public List<WardrobeItemResponse> getItems() {
        return items;
    }

    public void setItems(List<WardrobeItemResponse> items) {
        this.items = items;
    }
}
