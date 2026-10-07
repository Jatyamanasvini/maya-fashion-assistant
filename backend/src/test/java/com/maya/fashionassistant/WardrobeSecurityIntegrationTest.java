package com.maya.fashionassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maya.fashionassistant.dto.auth.LoginRequest;
import com.maya.fashionassistant.dto.auth.RegisterRequest;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemRequest;
import com.maya.fashionassistant.entity.ItemCategory;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WardrobeSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WardrobeItemRepository wardrobeItemRepository;

    @BeforeEach
    void setUp() {
        wardrobeItemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userB_CannotAccessOrModify_UserAWardrobeItem() throws Exception {
        // 1. Register User A
        RegisterRequest registerA = new RegisterRequest("User A", "usera@example.com", "passwordA123");
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerA)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = objectMapper.readTree(resA.getResponse().getContentAsString()).get("token").asText();

        // 2. Register User B
        RegisterRequest registerB = new RegisterRequest("User B", "userb@example.com", "passwordB123");
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerB)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = objectMapper.readTree(resB.getResponse().getContentAsString()).get("token").asText();

        // 3. User A creates a wardrobe item
        WardrobeItemRequest itemRequest = new WardrobeItemRequest();
        itemRequest.setCategory(ItemCategory.TOP);
        itemRequest.setDescription("User A Silk Blouse");
        itemRequest.setColor("ivory");
        itemRequest.setStyle("elegant");

        MvcResult createRes = mockMvc.perform(post("/api/wardrobe/items")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long itemAId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // 4. User A can retrieve their own item -> 200 OK
        mockMvc.perform(get("/api/wardrobe/items/" + itemAId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("User A Silk Blouse")));

        // 5. User B attempts to access User A's item -> 404 Not Found (Access Denied / Ownership Protected)
        mockMvc.perform(get("/api/wardrobe/items/" + itemAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 6. User B attempts to update User A's item -> 404 Not Found
        WardrobeItemRequest hackUpdate = new WardrobeItemRequest();
        hackUpdate.setCategory(ItemCategory.TOP);
        hackUpdate.setDescription("Hacked description");
        hackUpdate.setColor("black");

        mockMvc.perform(put("/api/wardrobe/items/" + itemAId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackUpdate)))
                .andExpect(status().isNotFound());

        // 7. User B attempts to delete User A's item -> 404 Not Found
        mockMvc.perform(delete("/api/wardrobe/items/" + itemAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 8. Verify User A's item is still intact
        mockMvc.perform(get("/api/wardrobe/items/" + itemAId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("User A Silk Blouse")));
    }
}
