package com.maya.fashionassistant.ai;

/**
 * Abstraction over an image-generation provider. Produces a visualization of the
 * selected outfit. The generated image is only a stylized visualization — it does
 * not guarantee photographic fidelity to the user's actual garments.
 */
public interface ImageGenerationClient {

    /**
     * Generates an outfit visualization image from a descriptive prompt.
     *
     * @param prompt descriptive text of the complete look
     * @return image bytes, or {@code null} when unavailable
     */
    byte[] generateImage(String prompt);
}
