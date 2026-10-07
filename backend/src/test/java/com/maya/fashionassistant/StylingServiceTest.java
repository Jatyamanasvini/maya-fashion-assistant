package com.maya.fashionassistant;

import com.maya.fashionassistant.ai.ChatAiClient;
import com.maya.fashionassistant.ai.ImageGenerationClient;
import com.maya.fashionassistant.dto.ai.OutfitSelectionItem;
import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.dto.chat.ChatReplyResponse;
import com.maya.fashionassistant.dto.outfit.OutfitItemResponse;
import com.maya.fashionassistant.dto.outfit.OutfitResponse;
import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.repository.OutfitRepository;
import com.maya.fashionassistant.repository.WardrobeItemRepository;
import com.maya.fashionassistant.service.ConversationService;
import com.maya.fashionassistant.service.ImageStorageService;
import com.maya.fashionassistant.service.IntentExtractor;
import com.maya.fashionassistant.service.OutfitService;
import com.maya.fashionassistant.service.OutfitVisualizationService;
import com.maya.fashionassistant.service.StylingService;
import com.maya.fashionassistant.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StylingServiceTest {

    @Mock
    private WardrobeItemRepository wardrobeItemRepository;
    @Mock
    private OutfitRepository outfitRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private OutfitService outfitService;
    @Mock
    private IntentExtractor intentExtractor;
    @Mock
    private WeatherService weatherService;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private ChatAiClient chatAiClient;
    @Mock
    private ImageGenerationClient imageGenerationClient;
    @Mock
    private OutfitVisualizationService outfitVisualizationService;

    private StylingService stylingService;

    @BeforeEach
    void setUp() {
        stylingService = new StylingService(
                wardrobeItemRepository, outfitRepository, conversationService, outfitService,
                intentExtractor, weatherService, imageStorageService, chatAiClient,
                imageGenerationClient, outfitVisualizationService, "COMPOSITE");
    }

    private WardrobeItem item(Long id, ItemCategory category, String desc) {
        WardrobeItem item = new WardrobeItem();
        item.setId(id);
        item.setUserId(1L);
        item.setCategory(category);
        item.setDescription(desc);
        item.setColor("black");
        item.setStyle("casual");
        item.setFormality("casual");
        return item;
    }

    @Test
    void handleMessage_RejectsForeignWardrobeId_AndFallsBackToOwnedItems() {
        // User owns items 1 (TOP), 2 (BOTTOM), 3 (SHOES)
        List<WardrobeItem> wardrobe = List.of(
                item(1L, ItemCategory.TOP, "Cream top"),
                item(2L, ItemCategory.BOTTOM, "Blue jeans"),
                item(3L, ItemCategory.SHOES, "Black loafers")
        );

        when(wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(wardrobe);
        when(intentExtractor.extract(any())).thenReturn(new com.maya.fashionassistant.service.StylingIntent());
        when(outfitRepository.findFirstByConversationIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
        when(weatherService.getWeather(null, null)).thenReturn(null);

        // AI returns a FOREIGN item id (999) not owned by the user
        OutfitSelectionResponse aiResponse = new OutfitSelectionResponse(
                List.of(new OutfitSelectionItem(999L, "TOP")), "should be ignored", "ignored hairstyle");
        when(chatAiClient.selectOutfit(anyList(), any(), anyList())).thenReturn(aiResponse);

        // No owned items match id 999
        when(wardrobeItemRepository.findByUserIdAndIdIn(eq(1L), anyList())).thenReturn(List.of());

        when(outfitService.createOutfit(eq(1L), eq(10L), any(), any(), anyList(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<OutfitService.OutfitSelection> selections = invocation.getArgument(4);
                    return new OutfitResponse(1L, "hairstyle", "reason", null, null,
                            selections.stream()
                                    .map(s -> new OutfitItemResponse(null, s.role(), s.wardrobeItemId(), null, null, "d", "c"))
                                    .toList());
                });

        ChatReplyResponse response = stylingService.handleMessage(1L, 10L, "outfit please", null, null);

        assertNotNull(response);
        assertNotNull(response.getOutfit());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OutfitService.OutfitSelection>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(outfitService).createOutfit(eq(1L), eq(10L), any(), any(), captor.capture(), any());

        List<OutfitService.OutfitSelection> selections = captor.getValue();
        assertFalse(selections.isEmpty());
        for (OutfitService.OutfitSelection selection : selections) {
            assertNotEquals(999L, selection.wardrobeItemId(), "Foreign AI-provided ID must be rejected");
        }
    }

    @Test
    void handleMessage_EmptyWardrobe_ReturnsGracefulReplyWithoutOutfit() {
        when(wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(intentExtractor.extract(any())).thenReturn(new com.maya.fashionassistant.service.StylingIntent());

        ChatReplyResponse response = stylingService.handleMessage(1L, 10L, "outfit please", null, null);

        assertNull(response.getOutfit());
        assertNotNull(response.getReply());
        verify(outfitService, never()).createOutfit(any(), any(), any(), any(), any(), any());
        verify(conversationService).saveMessage(10L, com.maya.fashionassistant.entity.MessageRole.USER, "outfit please");
    }

    @Test
    void handleMessage_ExcludesSneakers_WhenUserRejectsThem() {
        List<WardrobeItem> wardrobe = List.of(
                item(1L, ItemCategory.TOP, "Cream top"),
                item(2L, ItemCategory.BOTTOM, "Blue jeans"),
                item(3L, ItemCategory.SHOES, "White sneakers"),
                item(4L, ItemCategory.SHOES, "Black loafers")
        );

        com.maya.fashionassistant.service.StylingIntent intent = new com.maya.fashionassistant.service.StylingIntent();
        intent.addExclusion("sneakers");

        when(wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(wardrobe);
        when(intentExtractor.extract(any())).thenReturn(intent);
        when(outfitRepository.findFirstByConversationIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
        when(weatherService.getWeather(null, null)).thenReturn(null);
        when(chatAiClient.selectOutfit(anyList(), any(), anyList())).thenReturn(null); // force deterministic

        when(outfitService.createOutfit(eq(1L), eq(10L), any(), any(), anyList(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<OutfitService.OutfitSelection> selections = invocation.getArgument(4);
                    return new OutfitResponse(1L, "h", "r", null, null,
                            selections.stream()
                                    .map(s -> new OutfitItemResponse(null, s.role(), s.wardrobeItemId(), null, null, "d", "c"))
                                    .toList());
                });

        stylingService.handleMessage(1L, 10L, "I don't want sneakers.", null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OutfitService.OutfitSelection>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(outfitService).createOutfit(eq(1L), eq(10L), any(), any(), captor.capture(), any());

        List<OutfitService.OutfitSelection> selections = captor.getValue();
        assertTrue(selections.stream().noneMatch(s -> s.wardrobeItemId().equals(3L)),
                "Sneakers must be excluded from the recommendation");
    }
}
