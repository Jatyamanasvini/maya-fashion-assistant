package com.maya.fashionassistant.service;

import com.maya.fashionassistant.dto.outfit.OutfitItemResponse;
import com.maya.fashionassistant.dto.outfit.OutfitResponse;
import com.maya.fashionassistant.entity.Outfit;
import com.maya.fashionassistant.entity.OutfitItem;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.exception.ResourceNotFoundException;
import com.maya.fashionassistant.repository.OutfitItemRepository;
import com.maya.fashionassistant.repository.OutfitRepository;
import com.maya.fashionassistant.repository.WardrobeItemRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutfitService {

    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final WardrobeItemRepository wardrobeItemRepository;
    private final ImageStorageService imageStorageService;

    public OutfitService(
            OutfitRepository outfitRepository,
            OutfitItemRepository outfitItemRepository,
            WardrobeItemRepository wardrobeItemRepository,
            ImageStorageService imageStorageService) {
        this.outfitRepository = outfitRepository;
        this.outfitItemRepository = outfitItemRepository;
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public OutfitResponse createOutfit(Long userId, Long conversationId, String hairstyle, String reason,
                                       List<OutfitSelection> selections, String generatedImagePath) {
        Outfit outfit = new Outfit(userId, conversationId, hairstyle, reason, generatedImagePath);
        for (OutfitSelection selection : selections) {
            outfit.addItem(new OutfitItem(selection.wardrobeItemId(), selection.role()));
        }
        Outfit saved = outfitRepository.save(outfit);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OutfitResponse> getOutfits(Long userId) {
        List<Outfit> outfits = outfitRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return outfits.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OutfitResponse getOutfit(Long userId, Long outfitId) {
        Outfit outfit = outfitRepository.findByIdAndUserId(outfitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Outfit not found with id: " + outfitId));
        return toResponse(outfit);
    }

    @Transactional(readOnly = true)
    public Resource getGeneratedImage(Long userId, Long outfitId) {
        Outfit outfit = outfitRepository.findByIdAndUserId(outfitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Outfit not found with id: " + outfitId));

        if (!StringUtils.hasText(outfit.getGeneratedImagePath())) {
            throw new ResourceNotFoundException("No visualization generated for this outfit");
        }
        return imageStorageService.loadAsResource(outfit.getGeneratedImagePath());
    }

    private OutfitResponse toResponse(Outfit outfit) {
        List<OutfitItem> items = outfitItemRepository.findByOutfitId(outfit.getId());

        // Join wardrobe item metadata for display, enforcing user ownership.
        List<Long> ids = items.stream().map(OutfitItem::getWardrobeItemId).collect(Collectors.toList());
        Map<Long, WardrobeItem> wardrobeById = ids.isEmpty()
                ? Map.of()
                : wardrobeItemRepository.findByUserIdAndIdIn(outfit.getUserId(), ids).stream()
                        .collect(Collectors.toMap(WardrobeItem::getId, Function.identity()));

        List<OutfitItemResponse> itemResponses = items.stream().map(item -> {
            WardrobeItem wardrobe = wardrobeById.get(item.getWardrobeItemId());
            return new OutfitItemResponse(
                    item.getId(),
                    item.getRole(),
                    item.getWardrobeItemId(),
                    wardrobe != null ? wardrobe.getCategory() : null,
                    wardrobe != null ? wardrobe.getSubcategory() : null,
                    wardrobe != null ? wardrobe.getDescription() : null,
                    wardrobe != null ? wardrobe.getColor() : null
            );
        }).collect(Collectors.toList());

        String generatedImageUrl = StringUtils.hasText(outfit.getGeneratedImagePath())
                ? "/api/outfits/" + outfit.getId() + "/image"
                : null;

        return new OutfitResponse(
                outfit.getId(),
                outfit.getHairstyle(),
                outfit.getReason(),
                generatedImageUrl,
                outfit.getCreatedAt(),
                itemResponses
        );
    }

    /**
     * Immutable pairing of a validated wardrobe item ID and its outfit role.
     */
    public record OutfitSelection(Long wardrobeItemId, String role) {
    }
}
