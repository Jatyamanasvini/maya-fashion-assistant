package com.maya.fashionassistant.dto.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.maya.fashionassistant.entity.ItemCategory;

import java.io.IOException;
import java.util.Locale;

/**
 * Lenient deserializer for {@link ItemCategory}.
 *
 * <p>Smaller/local language models sometimes return a category that is almost right —
 * e.g. echoing the whole enum list ("TOP | BOTTOM | DRESS | ..."), adding quotes, or
 * using different casing. This deserializer:</p>
 * <ul>
 *   <li>accepts exact values case-insensitively,</li>
 *   <li>recovers a category when exactly one enum name appears in the value,</li>
 *   <li>returns {@code null} when the value is ambiguous, so the item is skipped rather
 *       than failing the whole request.</li>
 * </ul>
 */
public class ItemCategoryDeserializer extends JsonDeserializer<ItemCategory> {

    @Override
    public ItemCategory deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim().replace("\"", "").toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        // 1. Exact match
        for (ItemCategory category : ItemCategory.values()) {
            if (category.name().equals(normalized)) {
                return category;
            }
        }

        // 2. Best effort: if exactly one category name appears, use it.
        ItemCategory found = null;
        int matches = 0;
        for (ItemCategory category : ItemCategory.values()) {
            if (normalized.matches(".*\\b" + category.name() + "\\b.*")) {
                found = category;
                matches++;
            }
        }

        // 3. Ambiguous (e.g. the model echoed the whole enum list) -> skip the item.
        return matches == 1 ? found : null;
    }
}
