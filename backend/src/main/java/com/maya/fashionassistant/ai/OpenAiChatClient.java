package com.maya.fashionassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.exception.AiServiceException;
import com.maya.fashionassistant.service.StylingIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI-backed outfit ranking client. Uses the Chat Completions API with a
 * JSON response format. The returned wardrobe item IDs are treated as untrusted
 * and re-validated by the backend.
 */
@Component
public class OpenAiChatClient implements ChatAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public OpenAiChatClient(
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gpt-4o-mini}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public OutfitSelectionResponse selectOutfit(List<WardrobeItem> wardrobe, StylingIntent intent, List<Long> previousItemIds) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (wardrobe == null || wardrobe.isEmpty()) {
            return null;
        }

        String wardrobeListing = wardrobe.stream()
                .map(item -> String.format(
                        "{\"wardrobeItemId\":%d,\"category\":\"%s\",\"subcategory\":\"%s\",\"description\":\"%s\",\"color\":\"%s\",\"style\":\"%s\",\"formality\":\"%s\",\"season\":\"%s\"}",
                        item.getId(),
                        item.getCategory(),
                        nullToEmpty(item.getSubcategory()),
                        nullToEmpty(item.getDescription()),
                        nullToEmpty(item.getColor()),
                        nullToEmpty(item.getStyle()),
                        nullToEmpty(item.getFormality()),
                        nullToEmpty(item.getSeason())))
                .collect(Collectors.joining(",\n"));

        String systemPrompt = """
                You are Maya, a personal fashion stylist. You style complete outfits using ONLY the wardrobe items provided to you.
                You must NEVER invent or assume an item that is not in the provided list.

                Return strict JSON matching this schema (no prose, no markdown):
                {
                  "items": [
                    { "wardrobeItemId": <number from the list>, "role": "TOP|BOTTOM|DRESS|OUTERWEAR|SHOES|BAG|ACCESSORY|JEWELRY" }
                  ],
                  "reason": "1-3 sentences explaining why this combination works, referencing the actual selected items",
                  "hairstyle": "a specific hairstyle that complements the outfit and occasion"
                }

                Rules:
                - Select at most one item per role.
                - A DRESS replaces TOP and BOTTOM.
                - SHOES are strongly preferred when available.
                - Every wardrobeItemId MUST come from the provided list.
                """;

        String userPrompt = buildUserPrompt(intent, previousItemIds);

        Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMessage = Map.of("role", "user", "content",
                "Here is the user's actual wardrobe:\n[\n" + wardrobeListing + "\n]\n\n" + userPrompt);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(systemMessage, userMessage),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.3
        );

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (choices.isEmpty()) {
                return null;
            }

            String contentString = choices.get(0).path("message").path("content").asText();
            return objectMapper.readValue(contentString, OutfitSelectionResponse.class);
        } catch (Exception e) {
            log.warn("Outfit ranking AI call failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(StylingIntent intent, List<Long> previousItemIds) {
        StringBuilder sb = new StringBuilder("Styling request: ");
        if (intent.getOccasion() != null) sb.append("occasion=").append(intent.getOccasion()).append(", ");
        if (intent.getStyle() != null) sb.append("style=").append(intent.getStyle()).append(", ");
        if (intent.getFormality() != null) sb.append("formality=").append(intent.getFormality()).append(", ");
        if (intent.isMoreFeminine()) sb.append("make it more feminine, ");
        if (intent.isMoreCasual()) sb.append("make it more casual, ");
        if (intent.isMoreFormal()) sb.append("make it more formal, ");
        if (!intent.getExclusions().isEmpty()) {
            sb.append("AVOID these: ").append(String.join(", ", intent.getExclusions())).append(", ");
        }
        if (intent.isAnotherOption() && previousItemIds != null && !previousItemIds.isEmpty()) {
            sb.append("give a DIFFERENT option (do not reuse item IDs ")
                    .append(previousItemIds.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                    .append("), ");
        }
        if (sb.length() > "Styling request: ".length()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("\nSelect one complete outfit from the wardrobe list above.");
        return sb.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
