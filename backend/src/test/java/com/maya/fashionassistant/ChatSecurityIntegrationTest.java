package com.maya.fashionassistant;

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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WardrobeItemRepository wardrobeItemRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private OutfitRepository outfitRepository;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        outfitRepository.deleteAll();
        conversationRepository.deleteAll();
        wardrobeItemRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String register(String name, String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(name, email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createItem(String token, ItemCategory category, String description) throws Exception {
        WardrobeItemRequest item = new WardrobeItemRequest();
        item.setCategory(category);
        item.setDescription(description);
        item.setColor("black");
        item.setStyle("casual");
        item.setFormality("casual");
        MvcResult result = mockMvc.perform(post("/api/wardrobe/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void userB_CannotAccess_UserAConversationOrOutfit() throws Exception {
        String tokenA = register("User A", "chata@example.com", "passwordA123");
        String tokenB = register("User B", "chatb@example.com", "passwordB123");

        // User A populates wardrobe and asks for an outfit
        createItem(tokenA, ItemCategory.TOP, "Cream knit top");
        createItem(tokenA, ItemCategory.BOTTOM, "Blue denim jeans");
        createItem(tokenA, ItemCategory.SHOES, "Black loafers");

        MvcResult convResult = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Brunch look\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long conversationId = objectMapper.readTree(convResult.getResponse().getContentAsString()).get("id").asLong();

        ChatMessageRequest message = new ChatMessageRequest();
        message.setMessage("Pick something cute for brunch.");
        MvcResult replyResult = mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outfit", notNullValue()))
                .andReturn();

        long outfitId = objectMapper.readTree(replyResult.getResponse().getContentAsString())
                .get("outfit").get("id").asLong();

        // User A can access their own conversation and outfit
        mockMvc.perform(get("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/outfits/" + outfitId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // User B cannot access User A's conversation
        mockMvc.perform(get("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B cannot access User A's outfit
        mockMvc.perform(get("/api/outfits/" + outfitId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B cannot post a message into User A's conversation
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isNotFound());
    }
}
