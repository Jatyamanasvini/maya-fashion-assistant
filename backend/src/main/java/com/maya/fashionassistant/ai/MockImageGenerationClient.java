package com.maya.fashionassistant.ai;

import org.springframework.stereotype.Component;

/**
 * Returns {@code null} in mock/offline mode — no visualization is produced, and
 * the frontend gracefully omits the image block.
 */
@Component
public class MockImageGenerationClient implements ImageGenerationClient {

    @Override
    public byte[] generateImage(String prompt) {
        return null;
    }
}
