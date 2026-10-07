package com.maya.fashionassistant.ai;

import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
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
}
