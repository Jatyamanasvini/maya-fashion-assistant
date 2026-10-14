package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShoppingIdeaResponse {

    private List<ShoppingIdea> items = new ArrayList<>();

    public List<ShoppingIdea> getItems() {
        return items;
    }

    public void setItems(List<ShoppingIdea> items) {
        this.items = items;
    }
}
