package com.maya.fashionassistant.service;

import com.maya.fashionassistant.ai.VisionAiClient;
import com.maya.fashionassistant.dto.ai.VisionAnalysisResponse;
import com.maya.fashionassistant.dto.ai.VisionItemExtraction;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemRequest;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemResponse;
import com.maya.fashionassistant.dto.wardrobe.WardrobeUploadResponse;
import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.entity.WardrobeSource;
import com.maya.fashionassistant.exception.BadRequestException;
import com.maya.fashionassistant.exception.ResourceNotFoundException;
import com.maya.fashionassistant.repository.WardrobeItemRepository;
import com.maya.fashionassistant.repository.WardrobeSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WardrobeService {

    private static final Logger log = LoggerFactory.getLogger(WardrobeService.class);

    private final WardrobeItemRepository wardrobeItemRepository;
    private final WardrobeSourceRepository wardrobeSourceRepository;
    private final ImageStorageService imageStorageService;
    private final VisionAiClient visionAiClient;

    public WardrobeService(
            WardrobeItemRepository wardrobeItemRepository,
            WardrobeSourceRepository wardrobeSourceRepository,
            ImageStorageService imageStorageService,
            VisionAiClient visionAiClient) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.wardrobeSourceRepository = wardrobeSourceRepository;
        this.imageStorageService = imageStorageService;
        this.visionAiClient = visionAiClient;
    }

    @Transactional
    public WardrobeUploadResponse uploadAndProcessPhoto(Long userId, MultipartFile file) {
        // 1. Store original photograph securely
        String originalPath = imageStorageService.store(file, "original");

        // 2. Persist WardrobeSource record
        WardrobeSource source = new WardrobeSource(userId, originalPath);
        WardrobeSource savedSource = wardrobeSourceRepository.save(source);

        // 3. Extract items using Multimodal Vision AI
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read image bytes for user {}: {}", userId, e.getMessage());
            throw new BadRequestException("Could not read uploaded photo: " + e.getMessage());
        }

        VisionAnalysisResponse analysisResponse = visionAiClient.analyzeWardrobePhoto(imageBytes, file.getContentType());

        // 4. Validate and persist extracted wardrobe items
        List<WardrobeItem> savedItems = new ArrayList<>();
        if (analysisResponse != null && analysisResponse.getItems() != null) {
            for (VisionItemExtraction extracted : analysisResponse.getItems()) {
                if (isValidExtraction(extracted)) {
                    WardrobeItem item = new WardrobeItem(
                            userId,
                            savedSource.getId(),
                            extracted.getCategory(),
                            cleanString(extracted.getSubcategory(), 60),
                            cleanString(extracted.getDescription(), 500),
                            cleanString(extracted.getColor(), 50),
                            cleanString(extracted.getSecondaryColor(), 50),
                            cleanString(extracted.getPattern(), 50),
                            cleanString(extracted.getMaterial(), 50),
                            cleanString(extracted.getStyle(), 50),
                            cleanString(extracted.getFit(), 50),
                            cleanString(extracted.getFormality(), 50),
                            cleanString(extracted.getSeason(), 50),
                            originalPath // Links to source image reference
                    );
                    savedItems.add(wardrobeItemRepository.save(item));
                } else {
                    log.warn("Skipping invalid AI item extraction: category={}, desc={}",
                            extracted.getCategory(), extracted.getDescription());
                }
            }
        }

        List<WardrobeItemResponse> itemResponses = savedItems.stream()
                .map(WardrobeItemResponse::fromEntity)
                .collect(Collectors.toList());

        return new WardrobeUploadResponse(
                savedSource.getId(),
                savedSource.getOriginalImagePath(),
                savedSource.getCreatedAt(),
                itemResponses
        );
    }

    @Transactional(readOnly = true)
    public List<WardrobeItemResponse> getUserItems(Long userId) {
        return wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WardrobeItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WardrobeItemResponse getItemById(Long userId, Long itemId) {
        // Enforce strict ownership check
        WardrobeItem item = wardrobeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wardrobe item not found with id: " + itemId));

        return WardrobeItemResponse.fromEntity(item);
    }

    @Transactional
    public WardrobeItemResponse createItem(Long userId, WardrobeItemRequest request) {
        WardrobeItem item = new WardrobeItem(
                userId,
                null,
                request.getCategory(),
                request.getSubcategory(),
                request.getDescription(),
                request.getColor(),
                request.getSecondaryColor(),
                request.getPattern(),
                request.getMaterial(),
                request.getStyle(),
                request.getFit(),
                request.getFormality(),
                request.getSeason(),
                request.getImagePath()
        );
        return WardrobeItemResponse.fromEntity(wardrobeItemRepository.save(item));
    }

    @Transactional
    public WardrobeItemResponse updateItem(Long userId, Long itemId, WardrobeItemRequest request) {
        WardrobeItem item = wardrobeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wardrobe item not found with id: " + itemId));

        item.setCategory(request.getCategory());
        item.setSubcategory(request.getSubcategory());
        item.setDescription(request.getDescription());
        item.setColor(request.getColor());
        item.setSecondaryColor(request.getSecondaryColor());
        item.setPattern(request.getPattern());
        item.setMaterial(request.getMaterial());
        item.setStyle(request.getStyle());
        item.setFit(request.getFit());
        item.setFormality(request.getFormality());
        item.setSeason(request.getSeason());
        if (StringUtils.hasText(request.getImagePath())) {
            item.setImagePath(request.getImagePath());
        }

        return WardrobeItemResponse.fromEntity(wardrobeItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        WardrobeItem item = wardrobeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wardrobe item not found with id: " + itemId));

        wardrobeItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Resource getItemImageResource(Long userId, Long itemId) {
        WardrobeItem item = wardrobeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wardrobe item not found with id: " + itemId));

        if (!StringUtils.hasText(item.getImagePath())) {
            throw new ResourceNotFoundException("No image associated with this wardrobe item");
        }

        return imageStorageService.loadAsResource(item.getImagePath());
    }

    @Transactional(readOnly = true)
    public Resource getSourceImageResource(Long userId, Long sourceId) {
        WardrobeSource source = wardrobeSourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wardrobe photo not found with id: " + sourceId));

        return imageStorageService.loadAsResource(source.getOriginalImagePath());
    }

    private boolean isValidExtraction(VisionItemExtraction extracted) {
        return extracted != null
                && extracted.getCategory() != null
                && StringUtils.hasText(extracted.getDescription())
                && StringUtils.hasText(extracted.getColor());
    }

    private String cleanString(String input, int maxLength) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
