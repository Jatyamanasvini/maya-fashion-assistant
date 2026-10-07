package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single wardrobe item chosen by the AI for an outfit role.
 * The backend re-validates {@code wardrobeItemId} against the authenticated user's
 * wardrobe before trusting it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutfitSelectionItem {

    private Long wardrobeItemId;
    private String role;

    public OutfitSelectionItem() {
    }

    public OutfitSelectionItem(Long wardrobeItemId, String role) {
        this.wardrobeItemId = wardrobeItemId;
        this.role = role;
    }

    public Long getWardrobeItemId() {
        return wardrobeItemId;
    }

    public void setWardrobeItemId(Long wardrobeItemId) {
        this.wardrobeItemId = wardrobeItemId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
