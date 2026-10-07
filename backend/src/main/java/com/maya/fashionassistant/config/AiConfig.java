package com.maya.fashionassistant.config;

import com.maya.fashionassistant.ai.ChatAiClient;
import com.maya.fashionassistant.ai.ImageGenerationClient;
import com.maya.fashionassistant.ai.MockChatClient;
import com.maya.fashionassistant.ai.MockImageGenerationClient;
import com.maya.fashionassistant.ai.MockVisionAiClient;
import com.maya.fashionassistant.ai.OpenAiChatClient;
import com.maya.fashionassistant.ai.OpenAiImageGenerationClient;
import com.maya.fashionassistant.ai.OpenAiVisionClient;
import com.maya.fashionassistant.ai.PollinationsImageGenerationClient;
import com.maya.fashionassistant.ai.VisionAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * Selects the active AI provider implementations based on configuration.
 *
 * <p>Providers:</p>
 * <ul>
 *   <li>{@code OPENAI} — OpenAI cloud API (requires a key with credits).</li>
 *   <li>{@code OLLAMA} — a local, free Ollama server. Ollama exposes an
 *       OpenAI-compatible API, so the same OpenAI-compatible clients are reused;
 *       only {@code base-url}, {@code model} and a dummy {@code api-key} differ.
 *       Image generation is not supported by Ollama and stays in MOCK mode.</li>
 *   <li>{@code MOCK} — offline deterministic fallback (no external calls).</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${ai.provider:MOCK}")
    private String aiProvider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.image-gen.provider:MOCK}")
    private String imageGenProvider;

    @Value("${ai.image-gen.api-key:}")
    private String imageGenApiKey;

    @Bean
    @Primary
    public VisionAiClient activeVisionAiClient(
            OpenAiVisionClient openAiVisionClient,
            MockVisionAiClient mockVisionAiClient) {
        if (usesOpenAiCompatibleProvider()) {
            log.info("Configured active Vision AI provider: {}", providerLabel());
            return openAiVisionClient;
        }
        log.info("Configured active Vision AI provider: Mock (offline/testing mode)");
        return mockVisionAiClient;
    }

    @Bean
    @Primary
    public ChatAiClient activeChatAiClient(
            OpenAiChatClient openAiChatClient,
            MockChatClient mockChatClient) {
        if (usesOpenAiCompatibleProvider()) {
            log.info("Configured active outfit-ranking AI provider: {}", providerLabel());
            return openAiChatClient;
        }
        log.info("Configured active outfit-ranking AI provider: Mock (deterministic fallback)");
        return mockChatClient;
    }

    @Bean
    @Primary
    public ImageGenerationClient activeImageGenerationClient(
            OpenAiImageGenerationClient openAiImageGenerationClient,
            PollinationsImageGenerationClient pollinationsImageGenerationClient,
            MockImageGenerationClient mockImageGenerationClient) {
        if ("POLLINATIONS".equalsIgnoreCase(imageGenProvider)) {
            log.info("Configured active image-generation provider: Pollinations (free, no key)");
            return pollinationsImageGenerationClient;
        }
        if ("OPENAI".equalsIgnoreCase(imageGenProvider) && StringUtils.hasText(imageGenApiKey)) {
            log.info("Configured active image-generation provider: OpenAI");
            return openAiImageGenerationClient;
        }
        log.info("Configured active image-generation provider: Mock (no visualization)");
        return mockImageGenerationClient;
    }

    /**
     * Ollama implements the OpenAI-compatible chat/vision protocol, so the same
     * clients are used. OpenAI additionally requires a non-empty API key.
     */
    private boolean usesOpenAiCompatibleProvider() {
        if ("OLLAMA".equalsIgnoreCase(aiProvider)) {
            return true;
        }
        return "OPENAI".equalsIgnoreCase(aiProvider) && StringUtils.hasText(apiKey);
    }

    private String providerLabel() {
        if ("OLLAMA".equalsIgnoreCase(aiProvider)) {
            return "Ollama (local, OpenAI-compatible)";
        }
        if ("OPENAI".equalsIgnoreCase(aiProvider)) {
            return "OpenAI";
        }
        return "Mock";
    }
}
