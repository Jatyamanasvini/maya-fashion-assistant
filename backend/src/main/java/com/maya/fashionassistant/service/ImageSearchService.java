package com.maya.fashionassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Free, keyless image search via the Openverse API (aggregates Creative Commons
 * images). Used to illustrate "items to buy" suggestions with real photos.
 */
@Service
public class ImageSearchService {

    private static final Logger log = LoggerFactory.getLogger(ImageSearchService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ImageSearchService(
            @Value("${maya.image-search.base-url:https://api.openverse.org/v1}") String baseUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "MayaFashionAssistant/1.0 (learning project)")
                .build();
    }

    public record SearchResult(String imageUrl, String sourceUrl) {
    }

    public SearchResult findImage(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/images/")
                            .queryParam("q", query)
                            .queryParam("page_size", 1)
                            .queryParam("mature", false)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode results = objectMapper.readTree(body).path("results");
            if (results.isEmpty()) {
                return null;
            }
            JsonNode first = results.get(0);
            String image = first.path("thumbnail").asText(null);
            if (image == null || image.isBlank()) {
                image = first.path("url").asText(null);
            }
            if (image == null || image.isBlank()) {
                return null;
            }
            return new SearchResult(image, first.path("foreign_landing_url").asText(null));
        } catch (Exception e) {
            log.warn("Image search failed for '{}': {}", query, e.getMessage());
            return null;
        }
    }
}
