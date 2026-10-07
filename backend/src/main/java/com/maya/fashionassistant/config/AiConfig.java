package com.maya.fashionassistant.config;

import com.maya.fashionassistant.ai.MockVisionAiClient;
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
}
