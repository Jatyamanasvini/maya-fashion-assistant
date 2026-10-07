package com.maya.fashionassistant;

import com.maya.fashionassistant.ai.PollinationsImageGenerationClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in live test for free, keyless image generation via Pollinations.ai.
 * Skipped by default. Run with:
 *
 * <pre>MAYA_LIVE_IMAGEGEN=true mvn -Dtest=LiveImageGenerationTest test</pre>
 */
@EnabledIfEnvironmentVariable(named = "MAYA_LIVE_IMAGEGEN", matches = "true")
class LiveImageGenerationTest {

    @Test
    void pollinationsClient_GeneratesAnImage() {
        PollinationsImageGenerationClient client =
                new PollinationsImageGenerationClient("https://image.pollinations.ai", 512, 512);

        byte[] bytes = client.generateImage(
                "fashion lookbook flat lay of a cream ribbed knit top, blue high-waisted jeans and black leather loafers");

        assertNotNull(bytes, "Pollinations returned no image");
        assertTrue(bytes.length > 1000, "Image looks too small: " + bytes.length + " bytes");

        boolean jpeg = bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
        boolean png = bytes.length > 4 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
        assertTrue(jpeg || png, "Response was not a recognized image format");
        System.out.println("[live-imagegen] generated " + bytes.length + " bytes ("
                + (jpeg ? "jpeg" : "png") + ")");
    }
}
