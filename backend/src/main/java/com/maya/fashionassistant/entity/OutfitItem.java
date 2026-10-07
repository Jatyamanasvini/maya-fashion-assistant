package com.maya.fashionassistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "outfit_items")
public class OutfitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", nullable = false)
    @JsonIgnore
    private Outfit outfit;

    @Column(name = "wardrobe_item_id", nullable = false)
    private Long wardrobeItemId;

    @Column(nullable = false, length = 30)
    private String role; // TOP, BOTTOM, DRESS, SHOES, BAG, ACCESSORY, OUTERWEAR

    public OutfitItem() {
    }

    public OutfitItem(Long wardrobeItemId, String role) {
        this.wardrobeItemId = wardrobeItemId;
        this.role = role;
    }

    public OutfitItem(Outfit outfit, Long wardrobeItemId, String role) {
        this.outfit = outfit;
        this.wardrobeItemId = wardrobeItemId;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Outfit getOutfit() {
        return outfit;
    }

    public void setOutfit(Outfit outfit) {
        this.outfit = outfit;
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
