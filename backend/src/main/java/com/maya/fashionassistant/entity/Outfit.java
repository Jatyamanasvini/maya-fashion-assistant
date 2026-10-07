package com.maya.fashionassistant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outfits")
public class Outfit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(length = 255)
    private String hairstyle;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "generated_image_path", length = 500)
    private String generatedImagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "outfit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OutfitItem> items = new ArrayList<>();

    public Outfit() {
    }

    public Outfit(Long userId, Long conversationId, String hairstyle, String reason, String generatedImagePath) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.hairstyle = hairstyle;
        this.reason = reason;
        this.generatedImagePath = generatedImagePath;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(OutfitItem item) {
        items.add(item);
        item.setOutfit(this);
    }

    public void removeItem(OutfitItem item) {
        items.remove(item);
        item.setOutfit(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
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

    public String getGeneratedImagePath() {
        return generatedImagePath;
    }

    public void setGeneratedImagePath(String generatedImagePath) {
        this.generatedImagePath = generatedImagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OutfitItem> getItems() {
        return items;
    }

    public void setItems(List<OutfitItem> items) {
        this.items = items;
    }
}
