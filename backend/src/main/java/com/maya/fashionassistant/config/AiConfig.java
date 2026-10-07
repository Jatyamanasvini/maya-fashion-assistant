package com.maya.fashionassistant.config;

import com.maya.fashionassistant.ai.ChatAiClient;
import com.maya.fashionassistant.ai.ImageGenerationClient;
import com.maya.fashionassistant.ai.MockChatClient;
import com.maya.fashionassistant.ai.MockImageGenerationClient;
import com.maya.fashionassistant.ai.MockVisionAiClient;
import com.maya.fashionassistant.ai.OpenAiChatClient;
import com.maya.fashionassistant.ai.OpenAiImageGenerationClient;
import com.maya.fashionassistant.ai.OpenAiVisionClient;
import com.maya.fashionassistant.ai.VisionAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

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
        if ("OPENAI".equalsIgnoreCase(aiProvider) && StringUtils.hasText(apiKey)) {
            log.info("Configured active Vision AI provider: OpenAI");
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
        if ("OPENAI".equalsIgnoreCase(aiProvider) && StringUtils.hasText(apiKey)) {
            log.info("Configured active outfit-ranking AI provider: OpenAI");
            return openAiChatClient;
        }
        log.info("Configured active outfit-ranking AI provider: Mock (deterministic fallback)");
        return mockChatClient;
    }

    @Bean
    @Primary
    public ImageGenerationClient activeImageGenerationClient(
            OpenAiImageGenerationClient openAiImageGenerationClient,
            MockImageGenerationClient mockImageGenerationClient) {
        if ("OPENAI".equalsIgnoreCase(imageGenProvider) && StringUtils.hasText(imageGenApiKey)) {
            log.info("Configured active image-generation provider: OpenAI");
            return openAiImageGenerationClient;
        }
        log.info("Configured active image-generation provider: Mock (no visualization)");
        return mockImageGenerationClient;
    }
}
