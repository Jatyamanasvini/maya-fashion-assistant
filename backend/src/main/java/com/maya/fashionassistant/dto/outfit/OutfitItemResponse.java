package com.maya.fashionassistant.dto.outfit;

import com.maya.fashionassistant.entity.ItemCategory;

public class OutfitItemResponse {

    private Long id;
    private String role;
    private Long wardrobeItemId;
    private ItemCategory category;
    private String subcategory;
    private String description;
    private String color;

    public OutfitItemResponse() {
    }

    public OutfitItemResponse(Long id, String role, Long wardrobeItemId, ItemCategory category,
                              String subcategory, String description, String color) {
        this.id = id;
        this.role = role;
        this.wardrobeItemId = wardrobeItemId;
        this.category = category;
        this.subcategory = subcategory;
        this.description = description;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getWardrobeItemId() {
        return wardrobeItemId;
    }

    public void setWardrobeItemId(Long wardrobeItemId) {
        this.wardrobeItemId = wardrobeItemId;
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
}
