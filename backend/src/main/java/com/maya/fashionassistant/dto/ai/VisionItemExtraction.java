package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.maya.fashionassistant.entity.ItemCategory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VisionItemExtraction {

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

    public VisionItemExtraction() {
    }

    public VisionItemExtraction(ItemCategory category, String subcategory, String description,
                                String color, String secondaryColor, String pattern,
                                String material, String style, String fit,
                                String formality, String season) {
        this.category = category;
        this.subcategory = subcategory;
        this.description = description;
        this.color = color;
        this.secondaryColor = secondaryColor;
        this.pattern = pattern;
        this.material = material;
        this.style = style;
        this.fit = fit;
        this.formality = formality;
        this.season = season;
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
}
