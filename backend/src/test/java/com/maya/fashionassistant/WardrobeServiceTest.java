package com.maya.fashionassistant;

import com.maya.fashionassistant.ai.VisionAiClient;
import com.maya.fashionassistant.dto.ai.VisionAnalysisResponse;
import com.maya.fashionassistant.dto.ai.VisionItemExtraction;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemRequest;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemResponse;
import com.maya.fashionassistant.dto.wardrobe.WardrobeUploadResponse;
import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.entity.WardrobeSource;
import com.maya.fashionassistant.exception.ResourceNotFoundException;
import com.maya.fashionassistant.repository.WardrobeItemRepository;
import com.maya.fashionassistant.repository.WardrobeSourceRepository;
import com.maya.fashionassistant.service.ImageStorageService;
import com.maya.fashionassistant.service.WardrobeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WardrobeServiceTest {

    @Mock
    private WardrobeItemRepository wardrobeItemRepository;

    @Mock
    private WardrobeSourceRepository wardrobeSourceRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private VisionAiClient visionAiClient;

    private WardrobeService wardrobeService;

    @BeforeEach
    void setUp() {
        wardrobeService = new WardrobeService(
                wardrobeItemRepository,
                wardrobeSourceRepository,
                imageStorageService,
                visionAiClient
        );
    }

    @Test
    void uploadAndProcessPhoto_ShouldStoreImageAndExtractItems() {
        MockMultipartFile file = new MockMultipartFile(
                "photo", "outfit.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        when(imageStorageService.store(file, "original")).thenReturn("original/outfit-123.jpg");

        WardrobeSource source = new WardrobeSource(1L, "original/outfit-123.jpg");
        source.setId(10L);
        when(wardrobeSourceRepository.save(any(WardrobeSource.class))).thenReturn(source);

        VisionItemExtraction item1 = new VisionItemExtraction(
                ItemCategory.TOP, "Blouse", "White silk blouse",
                "white", null, "solid", "silk", "chic", "slim", "smart_casual", "summer"
        );
        when(visionAiClient.analyzeWardrobePhoto(any(), any()))
                .thenReturn(new VisionAnalysisResponse(List.of(item1)));

        WardrobeItem savedItem = new WardrobeItem(
                1L, 10L, ItemCategory.TOP, "Blouse", "White silk blouse",
                "white", null, "solid", "silk", "chic", "slim", "smart_casual", "summer", "original/outfit-123.jpg"
        );
        savedItem.setId(101L);
        when(wardrobeItemRepository.save(any(WardrobeItem.class))).thenReturn(savedItem);

        WardrobeUploadResponse response = wardrobeService.uploadAndProcessPhoto(1L, file);

        assertNotNull(response);
        assertEquals(10L, response.getSourceId());
        assertEquals("original/outfit-123.jpg", response.getOriginalImagePath());
        assertEquals(1, response.getItems().size());
        assertEquals(ItemCategory.TOP, response.getItems().get(0).getCategory());
        assertEquals("White silk blouse", response.getItems().get(0).getDescription());
    }

    @Test
    void getItemById_WhenItemBelongsToUser_ShouldReturnItem() {
        WardrobeItem item = new WardrobeItem();
        item.setId(5L);
        item.setUserId(1L);
        item.setCategory(ItemCategory.DRESS);
        item.setDescription("Summer Dress");
        item.setColor("yellow");

        when(wardrobeItemRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(item));

        WardrobeItemResponse response = wardrobeService.getItemById(1L, 5L);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals(ItemCategory.DRESS, response.getCategory());
    }

    @Test
    void getItemById_WhenItemBelongsToAnotherUser_ShouldThrowNotFound() {
        when(wardrobeItemRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wardrobeService.getItemById(1L, 5L));
    }
}
