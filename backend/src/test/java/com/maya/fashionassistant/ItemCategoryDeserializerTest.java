package com.maya.fashionassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.ai.VisionItemExtraction;
import com.maya.fashionassistant.entity.ItemCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemCategoryDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ItemCategory read(String categoryJson) throws Exception {
        VisionItemExtraction item = mapper.readValue(
                "{\"category\":" + categoryJson + "}", VisionItemExtraction.class);
        return item.getCategory();
    }

    @Test
    void exactValue() throws Exception {
        assertEquals(ItemCategory.TOP, read("\"TOP\""));
    }

    @Test
    void lowercaseAndWhitespace() throws Exception {
        assertEquals(ItemCategory.BAG, read("\"  bag  \""));
    }

    @Test
    void singleCategoryWithExtraWords() throws Exception {
        assertEquals(ItemCategory.SHOES, read("\"SHOES (footwear)\""));
    }

    @Test
    void ambiguousSchemaEcho_ReturnsNull() throws Exception {
        assertNull(read("\"TOP | BOTTOM | DRESS | OUTERWEAR | SHOES | BAG | ACCESSORY | JEWELRY | OTHER\""));
    }

    @Test
    void unknownValue_ReturnsNull() throws Exception {
        assertNull(read("\"not-a-category\""));
    }

    @Test
    void nullValue_ReturnsNull() throws Exception {
        assertNull(read("null"));
    }
}
