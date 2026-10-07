package com.maya.fashionassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

/**
 * OpenAI Images API client (endpoint {@code POST /v1/images/generations}).
 *
 * <p>Different OpenAI image models return the result differently:
 * DALL-E 3 returns a hosted {@code url}, while the newer {@code gpt-image-*}
 * family returns inline base64 ({@code b64_json}). This client supports both.</p>
 *
 * <p>Note: OpenAI's image generation does not accept reference images, so the
 * visualization is prompt-based rather than a faithful composite of the user's
 * actual item photos.</p>
 */
@Component
public class OpenAiImageGenerationClient implements ImageGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiImageGenerationClient.class);

    private final RestClient restClient;
    private final RestClient downloadClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public OpenAiImageGenerationClient(
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.image-gen.api-key:}") String apiKey,
            @Value("${ai.image-gen.model:dall-e-3}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.downloadClient = RestClient.create();
    }

    @Override
    public byte[] generateImage(String prompt) {
        if (apiKey == null || apiKey.isBlank() || prompt == null || prompt.isBlank()) {
            return null;
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "n", 1,
                "size", "1024x1024"
        );

        try {
            String responseBody = restClient.post()
                    .uri("/images/generations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            JsonNode data = objectMapper.readTree(responseBody).path("data");
            if (data.isEmpty()) {
                return null;
            }

            JsonNode first = data.get(0);

            // Newer models (e.g. gpt-image-1) return inline base64; DALL-E returns a URL.
            String b64 = first.path("b64_json").asText(null);
            if (b64 != null && !b64.isBlank()) {
                return Base64.getDecoder().decode(b64);
            }

            String url = first.path("url").asText(null);
            if (url == null || url.isBlank()) {
                return null;
            }

            return downloadClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.warn("Image generation failed: {}", e.getMessage());
            return null;
        }
    }
}
