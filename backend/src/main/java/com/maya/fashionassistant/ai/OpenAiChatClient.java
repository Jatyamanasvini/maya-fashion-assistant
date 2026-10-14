package com.maya.fashionassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.dto.ai.ShoppingIdea;
import com.maya.fashionassistant.dto.ai.ShoppingIdeaResponse;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.service.StylingIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible client for outfit ranking and shopping suggestions.
 *
 * <p>Resilient to provider churn: tries the configured model plus fallbacks, tolerates
 * providers that reject JSON mode, and logs the provider's response body on failure so
 * deployment logs are actionable. Returned wardrobe IDs are still re-validated by the
 * backend (this client never has the final say on ownership).</p>
 */
@Component
public class OpenAiChatClient implements ChatAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final List<String> models;
    private final String reasoningEffort;

    public OpenAiChatClient(
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gpt-4o-mini}") String model,
            @Value("${ai.fallback-models:}") String fallbackModels,
            @Value("${ai.reasoning-effort:}") String reasoningEffort,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.models = AiModelCandidates.build(model, fallbackModels);
        this.reasoningEffort = reasoningEffort;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public OutfitSelectionResponse selectOutfit(List<WardrobeItem> wardrobe, StylingIntent intent, List<Long> previousItemIds) {
        if (apiKey == null || apiKey.isBlank() || wardrobe == null || wardrobe.isEmpty()) {
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

        String userPrompt = "Here is the user's actual wardrobe:\n[\n" + wardrobeListing + "\n]\n\n"
                + buildUserPrompt(intent, previousItemIds);

        return callJson(systemPrompt, userPrompt, 0.3, OutfitSelectionResponse.class);
    }

    @Override
    public List<ShoppingIdea> suggestShoppingIdeas(String userMessage, StylingIntent intent) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        String systemPrompt = """
                You are Maya, a personal stylist. The user has NO wardrobe items yet.
                Suggest 4 specific pieces to buy that together would create a great look for their request.
                Return ONLY JSON matching: {"items":[{"category":"TOP","title":"Cropped black blazer","reason":"Adds polish to a casual look.","query":"women cropped black blazer"}]}
                Rules:
                - category is exactly ONE of: TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, BAG, ACCESSORY, JEWELRY.
                - query is a short image-search phrase (3-6 words).
                - reason is ONE short sentence.
                - Output JSON only.
                """;

        String userPrompt = "Styling request: " + (userMessage == null || userMessage.isBlank()
                ? "a stylish everyday outfit" : userMessage);

        ShoppingIdeaResponse parsed = callJson(systemPrompt, userPrompt, 0.4, ShoppingIdeaResponse.class);
        return parsed != null && parsed.getItems() != null ? parsed.getItems() : List.of();
    }

    /**
     * Calls the chat endpoint, trying each candidate model and, when a provider rejects
     * JSON mode, retrying once without it. Returns {@code null} if everything fails.
     */
    private <T> T callJson(String systemPrompt, String userPrompt, double temperature, Class<T> type) {
        Exception last = null;
        for (String candidate : models) {
            try {
                return attempt(candidate, true, systemPrompt, userPrompt, temperature, type);
            } catch (RestClientResponseException e) {
                String body = safeBody(e);
                log.warn("Chat model '{}' failed: {} {}", candidate, e.getStatusCode(), body);
                last = e;
                if (AiModelCandidates.looksLikeFormatError(body)) {
                    try {
                        return attempt(candidate, false, systemPrompt, userPrompt, temperature, type);
                    } catch (Exception retry) {
                        log.warn("Chat model '{}' (no json mode) failed: {}", candidate, retry.getMessage());
                        last = retry;
                    }
                }
            } catch (Exception e) {
                log.warn("Chat model '{}' error: {}", candidate, e.getMessage());
                last = e;
            }
        }
        log.warn("All chat models failed: {}", last != null ? last.getMessage() : "no models configured");
        return null;
    }

    private <T> T attempt(String model, boolean jsonMode, String systemPrompt, String userPrompt,
                          double temperature, Class<T> type) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        if (jsonMode) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            requestBody.put("reasoning_effort", reasoningEffort);
        }
        requestBody.put("temperature", temperature);

        String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        JsonNode choices = objectMapper.readTree(responseBody).path("choices");
        if (choices.isEmpty()) {
            return null;
        }
        String content = AiModelCandidates.extractJson(choices.get(0).path("message").path("content").asText());
        return objectMapper.readValue(content, type);
    }

    private String safeBody(RestClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            return body == null ? "" : body.substring(0, Math.min(body.length(), 500));
        } catch (Exception ignored) {
            return "";
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
