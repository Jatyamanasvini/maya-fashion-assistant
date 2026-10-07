package com.maya.fashionassistant;

import com.maya.fashionassistant.ai.ChatAiClient;
import com.maya.fashionassistant.ai.OpenAiChatClient;
import com.maya.fashionassistant.ai.OpenAiVisionClient;
import com.maya.fashionassistant.ai.VisionAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that activating the {@code ollama} profile selects the OpenAI-compatible
 * clients (Ollama speaks that protocol) instead of the MOCK fallback. No network call
 * is made — Ollama only needs to be running when an actual request is issued.
 */
@SpringBootTest
@ActiveProfiles({"test", "ollama"})
class OllamaProfileWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void ollamaProfile_WiresOpenAiCompatibleClients() {
        assertTrue(context.getBean(VisionAiClient.class) instanceof OpenAiVisionClient,
                "Vision AI should use the OpenAI-compatible client under the ollama profile");
        assertTrue(context.getBean(ChatAiClient.class) instanceof OpenAiChatClient,
                "Chat/ranking AI should use the OpenAI-compatible client under the ollama profile");
    }
}
