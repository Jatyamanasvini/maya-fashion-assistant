package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VisionAnalysisResponse {

    private List<VisionItemExtraction> items = new ArrayList<>();

    public VisionAnalysisResponse() {
    }

    public VisionAnalysisResponse(List<VisionItemExtraction> items) {
        this.items = items;
    }

    public List<VisionItemExtraction> getItems() {
        return items;
    }

    public void setItems(List<VisionItemExtraction> items) {
        this.items = items;
    }
}
