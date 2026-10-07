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

import java.time.Duration;
import java.util.*;

@Component
public class OpenAiVisionClient implements VisionAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiVisionClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public OpenAiVisionClient(
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.vision-model:gpt-4o-mini}") String model,
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
    public VisionAnalysisResponse analyzeWardrobePhoto(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("AI API key is not configured");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + (mimeType != null ? mimeType : "image/jpeg") + ";base64," + base64Image;

        String systemPrompt = """
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
                      "season": "spring"
                    }
                  ]
                }
                Rules:
                - "category" MUST be exactly ONE single word chosen from: TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, BAG, ACCESSORY, JEWELRY, OTHER.
                - Every other field must be a single short lowercase word (or null when unknown).
                - Only list items clearly visible. Do not invent items. Ignore people, faces, and background.
                - Output JSON only. Do not explain.
                """;

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", "Analyze this image and identify all clothing, shoes, and accessories worn or shown."
        );

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl)
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent)
        );

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", systemPrompt
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("response_format", Map.of("type", "json_object"));
        requestBody.put("temperature", 0.2);

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new AiServiceException("Received empty response from Vision AI provider");
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (choices.isEmpty()) {
                throw new AiServiceException("No choices returned from Vision AI provider");
            }

            String contentString = choices.get(0).path("message").path("content").asText();
            VisionAnalysisResponse analysisResponse = objectMapper.readValue(contentString, VisionAnalysisResponse.class);

            if (analysisResponse == null || analysisResponse.getItems() == null) {
                return new VisionAnalysisResponse(Collections.emptyList());
            }

            return analysisResponse;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to execute Vision AI analysis: {}", e.getMessage(), e);
            throw new AiServiceException("Failed to analyze wardrobe photo: " + e.getMessage(), e);
        }
    }
}
