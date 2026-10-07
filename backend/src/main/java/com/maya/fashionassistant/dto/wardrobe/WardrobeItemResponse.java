package com.maya.fashionassistant.dto.wardrobe;

import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.WardrobeItem;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public class WardrobeItemResponse {

    private Long id;
    private Long sourceId;
    private ItemCategory category;
    private String subcategory;
    private String description;
    private String color;
    private String secondaryColor;
    private String pattern;
    private String material;
    private String style;
    private String fit;
    private String formality;
    private String season;
    private boolean hasImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WardrobeItemResponse() {
    }

    public static WardrobeItemResponse fromEntity(WardrobeItem item) {
        WardrobeItemResponse dto = new WardrobeItemResponse();
        dto.setId(item.getId());
        dto.setSourceId(item.getSourceId());
        dto.setCategory(item.getCategory());
        dto.setSubcategory(item.getSubcategory());
        dto.setDescription(item.getDescription());
        dto.setColor(item.getColor());
        dto.setSecondaryColor(item.getSecondaryColor());
        dto.setPattern(item.getPattern());
        dto.setMaterial(item.getMaterial());
        dto.setStyle(item.getStyle());
        dto.setFit(item.getFit());
        dto.setFormality(item.getFormality());
        dto.setSeason(item.getSeason());
        dto.setHasImage(StringUtils.hasText(item.getImagePath()));
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getFit() {
        return fit;
    }

    public void setFit(String fit) {
        this.fit = fit;
    }

    public String getFormality() {
        return formality;
    }

    public void setFormality(String formality) {
        this.formality = formality;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
