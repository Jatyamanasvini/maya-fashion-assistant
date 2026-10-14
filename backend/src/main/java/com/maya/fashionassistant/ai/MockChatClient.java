package com.maya.fashionassistant.ai;

import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.dto.ai.ShoppingIdea;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.service.StylingIntent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Returns {@code null} to signal "AI unavailable — use deterministic selection".
 * Keeps the styling pipeline functional in offline/test mode.
 */
@Component
public class MockChatClient implements ChatAiClient {

    @Override
    public OutfitSelectionResponse selectOutfit(List<WardrobeItem> wardrobe, StylingIntent intent, List<Long> previousItemIds) {
        return null;
    }

    @Override
    public List<ShoppingIdea> suggestShoppingIdeas(String userMessage, StylingIntent intent) {
        return List.of(
                idea("TOP", "Fitted ribbed knit top", "A simple base layer that works for most occasions.", "women ribbed knit top"),
                idea("BOTTOM", "High-waisted straight jeans", "Versatile and easy to dress up or down.", "women high waisted straight jeans"),
                idea("SHOES", "Everyday white leather sneakers", "Comfortable and pairs with almost everything.", "white leather sneakers women"),
                idea("BAG", "Structured leather shoulder bag", "Adds polish to casual looks.", "structured leather shoulder bag")
        );
    }

    private ShoppingIdea idea(String category, String title, String reason, String query) {
        ShoppingIdea idea = new ShoppingIdea();
        idea.setCategory(category);
        idea.setTitle(title);
        idea.setReason(reason);
        idea.setQuery(query);
        return idea;
    }
}
