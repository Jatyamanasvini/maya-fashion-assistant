package com.maya.fashionassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.ai.VisionAnalysisResponse;
import com.maya.fashionassistant.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Component
public class OpenAiVisionClient implements VisionAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiVisionClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final List<String> models;

    public OpenAiVisionClient(
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.vision-model:gpt-4o-mini}") String model,
            @Value("${ai.fallback-models:}") String fallbackModels,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.models = AiModelCandidates.build(model, fallbackModels);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public VisionAnalysisResponse analyzeWardrobePhoto(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("AI API key is not configured");
        }

        String dataUrl = "data:" + (mimeType != null ? mimeType : "image/jpeg") + ";base64,"
                + Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> systemMessage = Map.of("role", "system", "content", SYSTEM_PROMPT);
        Map<String, Object> userMessage = Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", "Analyze this image and identify all clothing, shoes, and accessories worn or shown."),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))));

        Exception last = null;
        for (String candidate : models) {
            try {
                return callModel(candidate, true, systemMessage, userMessage);
            } catch (RestClientResponseException e) {
                String body = safeBody(e);
                log.warn("Vision model '{}' failed: {} {}", candidate, e.getStatusCode(), body);
                last = e;
                if (AiModelCandidates.looksLikeFormatError(body)) {
                    try {
                        return callModel(candidate, false, systemMessage, userMessage);
                    } catch (Exception retry) {
                        log.warn("Vision model '{}' (no json mode) failed: {}", candidate, retry.getMessage());
                        last = retry;
                    }
                }
            } catch (Exception e) {
                log.warn("Vision model '{}' error: {}", candidate, e.getMessage());
                last = e;
            }
        }
        throw new AiServiceException("All vision models failed: "
                + (last != null ? last.getMessage() : "no models configured"), last);
    }

    private VisionAnalysisResponse callModel(String model, boolean jsonMode,
                                              Map<String, Object> systemMessage,
                                              Map<String, Object> userMessage) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        if (jsonMode) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }
        requestBody.put("temperature", 0.2);
        requestBody.put("max_tokens", 900);

        String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new AiServiceException("Empty response from vision provider");
        }
        JsonNode choices = objectMapper.readTree(responseBody).path("choices");
        if (choices.isEmpty()) {
            throw new AiServiceException("No choices from vision provider");
        }
        String content = AiModelCandidates.extractJson(choices.get(0).path("message").path("content").asText());
        VisionAnalysisResponse parsed = objectMapper.readValue(content, VisionAnalysisResponse.class);
        return parsed == null || parsed.getItems() == null ? new VisionAnalysisResponse(Collections.emptyList()) : parsed;
    }

    private String safeBody(RestClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            return body == null ? "" : body.substring(0, Math.min(body.length(), 500));
        } catch (Exception ignored) {
            return "";
        }
    }

    private static final String SYSTEM_PROMPT = """
            You are Maya, an expert fashion stylist and clothing identification assistant.
            Analyze the user's photograph and identify each distinct clothing, footwear, bag, and fashion accessory item.
            Return ONLY valid JSON matching exactly this structure (one object per identified item):
            {
              "items": [
                {
                  "category": "TOP",
                  "subcategory": "blouse",
                  "description": "Grey graphic off-shoulder cotton top",
                  "color": "grey",
                  "secondaryColor": null,
                  "pattern": "graphic",
                  "material": "cotton",
                  "style": "casual",
                  "fit": "regular",
                  "formality": "casual",
                  "season": "spring",
                  "bbox": [0.35, 0.19, 0.65, 0.38]
                }
              ]
            }
            Rules:
            - "category" MUST be exactly ONE single word chosen from: TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, BAG, ACCESSORY, JEWELRY, OTHER.
            - "bbox" is REQUIRED: the item's location as [x1,y1,x2,y2] normalized fractions of the image width and height, each between 0.0 and 1.0.
            - Every other field must be a single short lowercase word (or null when unknown).
            - Identify EVERY distinct garment or accessory, even if partly occluded.
            - Only list items clearly visible. Do not invent items. Ignore people, faces, hair, and background.
            - Output JSON only. Do not explain.
            """;
}
