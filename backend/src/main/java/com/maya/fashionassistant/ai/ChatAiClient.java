package com.maya.fashionassistant.ai;

import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.dto.ai.ShoppingIdea;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.service.StylingIntent;

import java.util.List;

/**
 * Abstraction for the AI responsible for outfit ranking, occasion reasoning,
 * explanation, and hairstyle suggestion. Implementations MUST ONLY select from
 * the wardrobe items supplied — never invent clothing.
 */
public interface ChatAiClient {

    /**
     * Selects a coherent outfit from the user's actual wardrobe items.
     *
     * @param wardrobe         the authenticated user's wardrobe items
     * @param intent           structured styling intent (occasion, style, constraints)
     * @param previousItemIds  item IDs used in the current/last outfit, to support "another option"
     * @return a structured selection, or {@code null} when unavailable (caller falls back to deterministic selection)
     */
    OutfitSelectionResponse selectOutfit(List<WardrobeItem> wardrobe, StylingIntent intent, List<Long> previousItemIds);

    /**
     * Suggests items the user could BUY when their wardrobe cannot fulfil the request.
     * These are explicitly external suggestions — never presented as owned items.
     *
     * @return a list of ideas (without images), or an empty list when unavailable
     */
    List<ShoppingIdea> suggestShoppingIdeas(String userMessage, StylingIntent intent);
}
