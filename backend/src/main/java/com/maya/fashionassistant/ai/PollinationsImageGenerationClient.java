package com.maya.fashionassistant.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Free, keyless image generation via Pollinations.ai.
 *
 * <p>Pollinations exposes a text-to-image endpoint at
 * {@code GET {base-url}/prompt/{url-encoded-prompt}} returning image bytes directly.
 * No API key or account is required.</p>
 *
 * <p>Like the OpenAI provider, this is prompt-based and does not accept reference
 * images, so the visualization is a stylistic interpretation rather than a faithful
 * composite of the user's actual garments.</p>
 */
@Component
public class PollinationsImageGenerationClient implements ImageGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(PollinationsImageGenerationClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final int width;
    private final int height;

    public PollinationsImageGenerationClient(
            @Value("${ai.image-gen.base-url:https://image.pollinations.ai}") String baseUrl,
            @Value("${ai.image-gen.width:1024}") int width,
            @Value("${ai.image-gen.height:1024}") int height) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.width = width;
        this.height = height;
        this.restClient = RestClient.create();
    }

    @Override
    public byte[] generateImage(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return null;
        }
        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
            int seed = Math.abs(prompt.hashCode() % 1_000_000);
            String url = baseUrl + "/prompt/" + encodedPrompt
                    + "?width=" + width + "&height=" + height + "&nologo=true&seed=" + seed;

            byte[] bytes = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);

            if (bytes == null || bytes.length == 0) {
                log.warn("Pollinations returned an empty image");
                return null;
            }
            return bytes;
        } catch (Exception e) {
            log.warn("Pollinations image generation failed: {}", e.getMessage());
            return null;
        }
    }
}
