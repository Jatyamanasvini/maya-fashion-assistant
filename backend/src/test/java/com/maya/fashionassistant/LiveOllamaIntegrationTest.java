package com.maya.fashionassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.ai.OpenAiChatClient;
import com.maya.fashionassistant.ai.OpenAiVisionClient;
import com.maya.fashionassistant.dto.ai.OutfitSelectionItem;
import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.dto.ai.VisionAnalysisResponse;
import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.service.StylingIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in live test that runs MAYA's real AI clients against a locally hosted,
 * free Ollama server (OpenAI-compatible API). Skipped by default.
 *
 * <p>Run with:</p>
 * <pre>
 *   MAYA_LIVE_OLLAMA=true mvn -Dtest=LiveOllamaIntegrationTest test
 * </pre>
 *
 * Requires Ollama running on localhost:11434 with a vision model pulled
 * (default {@code qwen2.5vl:3b}; override with {@code OLLAMA_MODEL}).
 */
@EnabledIfEnvironmentVariable(named = "MAYA_LIVE_OLLAMA", matches = "true")
class LiveOllamaIntegrationTest {

    private static final String BASE_URL = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1");
    private static final String MODEL = System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5vl:3b");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WardrobeItem item(long id, ItemCategory category, String desc, String color, String style) {
        WardrobeItem i = new WardrobeItem();
        i.setId(id);
        i.setUserId(1L);
        i.setCategory(category);
        i.setDescription(desc);
        i.setColor(color);
        i.setStyle(style);
        i.setFormality("casual");
        i.setSeason("all");
        return i;
    }

    @Test
    void chatClient_SelectsOutfit_FromRealWardrobe_AgainstOllama() {
        List<WardrobeItem> wardrobe = List.of(
                item(1L, ItemCategory.TOP, "Cream ribbed knit top", "cream", "casual"),
                item(2L, ItemCategory.BOTTOM, "Blue high-waisted jeans", "blue", "casual"),
                item(3L, ItemCategory.SHOES, "Black leather loafers", "black", "chic"),
                item(4L, ItemCategory.BAG, "Tan leather shoulder bag", "tan", "chic"),
                item(5L, ItemCategory.SHOES, "White running sneakers", "white", "sporty")
        );

        StylingIntent intent = new StylingIntent();
        intent.setOccasion("brunch");
        intent.setStyle("cute");
        intent.setFormality("casual");

        OpenAiChatClient client = new OpenAiChatClient(BASE_URL, "ollama", MODEL, MAPPER);
        OutfitSelectionResponse response = client.selectOutfit(wardrobe, intent, List.of());

        System.out.println("[live-ollama] chat selection: " + response);
        assertNotNull(response, "Ollama chat client returned null (request/response or JSON parsing failed)");
        assertNotNull(response.getItems(), "Ollama returned no items list");

        Set<Long> ownedIds = wardrobe.stream().map(WardrobeItem::getId).collect(Collectors.toSet());
        for (OutfitSelectionItem selected : response.getItems()) {
            assertTrue(ownedIds.contains(selected.getWardrobeItemId()),
                    "Model returned a wardrobeItemId not in the provided list: " + selected.getWardrobeItemId());
        }
        assertNotNull(response.getHairstyle(), "Expected a hairstyle suggestion");
    }

    @Test
    void visionClient_ParsesImage_AgainstOllama() throws Exception {
        byte[] image = syntheticTopImage();

        OpenAiVisionClient client = new OpenAiVisionClient(BASE_URL, "ollama", MODEL, MAPPER);
        VisionAnalysisResponse response = client.analyzeWardrobePhoto(image, "image/png");

        System.out.println("[live-ollama] vision response: "
                + (response == null ? "null" : response.getItems().size() + " item(s)"));
        assertNotNull(response, "Ollama vision client returned null (request/response or JSON parsing failed)");
        assertNotNull(response.getItems(), "Expected a structured items list");
    }

    /** Draws a crude blue top on a white background to exercise the vision pipeline. */
    private byte[] syntheticTopImage() throws Exception {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 256, 256);
        g.setColor(new Color(70, 110, 180));
        g.fillPolygon(new int[]{60, 90, 100, 156, 166, 196, 176, 166, 90, 80},
                new int[]{70, 55, 60, 60, 55, 70, 90, 220, 220, 90}, 10);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
