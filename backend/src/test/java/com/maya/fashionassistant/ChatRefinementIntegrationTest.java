package com.maya.fashionassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.auth.RegisterRequest;
import com.maya.fashionassistant.dto.chat.ChatMessageRequest;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemRequest;
import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.repository.ChatMessageRepository;
import com.maya.fashionassistant.repository.ConversationRepository;
import com.maya.fashionassistant.repository.OutfitRepository;
import com.maya.fashionassistant.repository.UserRepository;
import com.maya.fashionassistant.repository.WardrobeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Follow-up / refinement messages must work in the same conversation. This guards the
 * regression where reading the previous outfit's lazy {@code items} threw
 * LazyInitializationException on the second message, returning HTTP 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatRefinementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private WardrobeItemRepository wardrobeItemRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private OutfitRepository outfitRepository;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        outfitRepository.deleteAll();
        conversationRepository.deleteAll();
        wardrobeItemRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Refiner", email, "secret123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private void createItem(String token, ItemCategory category, String description) throws Exception {
        WardrobeItemRequest item = new WardrobeItemRequest();
        item.setCategory(category);
        item.setDescription(description);
        item.setColor("white");
        item.setStyle("casual");
        item.setFormality("casual");
        mockMvc.perform(post("/api/wardrobe/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated());
    }

    private MvcResult send(String token, long conversationId, String message) throws Exception {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage(message);
        return mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void followUpMessage_ReadsPreviousOutfit_AndAppliesExclusion() throws Exception {
        String token = register("refiner@example.com");

        createItem(token, ItemCategory.TOP, "Cream knit top");
        createItem(token, ItemCategory.BOTTOM, "Blue jeans");
        createItem(token, ItemCategory.SHOES, "White sneakers");

        MvcResult conv = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Refinement\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long conversationId = objectMapper.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        // First message creates an outfit (deterministic MOCK selection includes sneakers).
        MvcResult first = send(token, conversationId, "Pick something cute for brunch.");
        assertNotNull(objectMapper.readTree(first.getResponse().getContentAsString()).get("outfit"));

        // Second message must succeed (previously LazyInitializationException -> 500)
        // and must exclude sneakers.
        MvcResult second = send(token, conversationId, "I don't want sneakers.");
        JsonNode outfit = objectMapper.readTree(second.getResponse().getContentAsString()).get("outfit");

        assertNotNull(outfit);
        for (JsonNode item : outfit.get("items")) {
            assertNotEquals("SHOES", item.get("role").asText());
        }
    }
}
