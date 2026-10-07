package com.maya.fashionassistant.ai;

import com.maya.fashionassistant.dto.ai.VisionAnalysisResponse;
import com.maya.fashionassistant.dto.ai.VisionItemExtraction;
import com.maya.fashionassistant.entity.ItemCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockVisionAiClient implements VisionAiClient {

    private int callCounter = 0;

    @Override
    public VisionAnalysisResponse analyzeWardrobePhoto(byte[] imageBytes, String mimeType) {
        callCounter++;
        List<VisionItemExtraction> items = new ArrayList<>();

        if (callCounter % 3 == 1) {
            // Casual chic outfit
            items.add(new VisionItemExtraction(
                    ItemCategory.TOP, "Blouse", "Cream ribbed mock-neck knit top",
                    "cream", "white", "ribbed", "knit", "casual", "slim", "casual", "spring"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.BOTTOM, "Jeans", "High-waisted straight leg blue denim jeans",
                    "blue", null, "solid", "denim", "casual", "regular", "casual", "all"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.SHOES, "Loafers", "Chunky black leather loafers",
                    "black", null, "solid", "leather", "chic", "regular", "smart_casual", "all"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.BAG, "Shoulder Bag", "Tan structured leather shoulder bag",
                    "tan", "gold", "solid", "leather", "chic", "regular", "casual", "all"
            ));
        } else if (callCounter % 3 == 2) {
            // Brunch floral dress outfit
            items.add(new VisionItemExtraction(
                    ItemCategory.DRESS, "Midi Dress", "Soft pastel floral tiered midi sundress",
                    "pink", "green", "floral", "cotton", "feminine", "relaxed", "casual", "summer"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.OUTERWEAR, "Denim Jacket", "Classic light wash cropped denim jacket",
                    "light blue", null, "solid", "denim", "casual", "cropped", "casual", "spring"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.SHOES, "Sandals", "Neutral strappy block-heel sandals",
                    "beige", null, "solid", "leather", "feminine", "regular", "smart_casual", "summer"
            ));
        } else {
            // Smart casual dinner look
            items.add(new VisionItemExtraction(
                    ItemCategory.OUTERWEAR, "Blazer", "Oversized tailored charcoal houndstooth blazer",
                    "charcoal", "black", "houndstooth", "wool", "chic", "oversized", "smart_casual", "autumn"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.TOP, "Camisole", "Black silk minimalist cowl-neck camisole",
                    "black", null, "solid", "silk", "elegant", "slim", "smart_casual", "all"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.BOTTOM, "Trousers", "Pleated wide-leg beige tailored trousers",
                    "beige", null, "solid", "cotton blend", "elegant", "wide-leg", "smart_casual", "all"
            ));
            items.add(new VisionItemExtraction(
                    ItemCategory.SHOES, "Ankle Boots", "Pointed-toe black leather ankle boots",
                    "black", null, "solid", "leather", "chic", "slim", "smart_casual", "autumn"
            ));
        }

        return new VisionAnalysisResponse(items);
    }
}
