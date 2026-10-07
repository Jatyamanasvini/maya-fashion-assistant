package com.maya.fashionassistant.ai;

import com.maya.fashionassistant.dto.ai.VisionAnalysisResponse;

public interface VisionAiClient {

    /**
     * Analyzes an uploaded photograph and extracts visible wardrobe items and their attributes in structured format.
     *
     * @param imageBytes the raw image bytes
     * @param mimeType the MIME type of the image (e.g. "image/jpeg", "image/png")
     * @return structured VisionAnalysisResponse containing identified clothing/accessory items
     */
    VisionAnalysisResponse analyzeWardrobePhoto(byte[] imageBytes, String mimeType);
}
