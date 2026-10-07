package com.maya.fashionassistant.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, structured interpretation of a user's styling message.
 * Java is the authority here — the AI may enrich reasoning but never the
 * constraints used to filter the user's actual wardrobe.
 */
public class StylingIntent {

    private String occasion;               // e.g. "brunch", "dinner", "work", "party"
    private String formality;              // e.g. "casual", "smart_casual", "formal"
    private String style;                  // e.g. "cute", "feminine", "elegant", "cozy"
    private boolean moreFeminine;
    private boolean moreCasual;
    private boolean moreFormal;
    private boolean anotherOption;
    private final List<String> exclusions = new ArrayList<>(); // categories/subcategories/keywords to avoid

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public String getFormality() {
        return formality;
    }

    public void setFormality(String formality) {
        this.formality = formality;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public boolean isMoreFeminine() {
        return moreFeminine;
    }

    public void setMoreFeminine(boolean moreFeminine) {
        this.moreFeminine = moreFeminine;
    }

    public boolean isMoreCasual() {
        return moreCasual;
    }

    public void setMoreCasual(boolean moreCasual) {
        this.moreCasual = moreCasual;
    }

    public boolean isMoreFormal() {
        return moreFormal;
    }

    public void setMoreFormal(boolean moreFormal) {
        this.moreFormal = moreFormal;
    }

    public boolean isAnotherOption() {
        return anotherOption;
    }

    public void setAnotherOption(boolean anotherOption) {
        this.anotherOption = anotherOption;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void addExclusion(String exclusion) {
        this.exclusions.add(exclusion);
    }
}
