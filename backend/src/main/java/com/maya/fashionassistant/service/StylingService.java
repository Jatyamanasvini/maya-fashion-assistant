package com.maya.fashionassistant.service;

import com.maya.fashionassistant.ai.ChatAiClient;
import com.maya.fashionassistant.ai.ImageGenerationClient;
import com.maya.fashionassistant.dto.ai.OutfitSelectionItem;
import com.maya.fashionassistant.dto.ai.OutfitSelectionResponse;
import com.maya.fashionassistant.dto.chat.ChatReplyResponse;
import com.maya.fashionassistant.dto.outfit.OutfitResponse;
import com.maya.fashionassistant.dto.shopping.ShoppingSuggestion;
import com.maya.fashionassistant.dto.weather.WeatherInfo;
import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.MessageRole;
import com.maya.fashionassistant.entity.Outfit;
import com.maya.fashionassistant.entity.OutfitItem;
import com.maya.fashionassistant.entity.WardrobeItem;
import com.maya.fashionassistant.repository.OutfitRepository;
import com.maya.fashionassistant.repository.WardrobeItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the conversational styling pipeline:
 *
 * <pre>
 * user message
 *   -> persist message
 *   -> extract intent (deterministic)
 *   -> retrieve the user's ACTUAL wardrobe from MySQL
 *   -> AI ranks candidates (or deterministic fallback)
 *   -> validate every selected ID against DB ownership + role
 *   -> persist outfit
 *   -> hairstyle recommendation
 *   -> (optional) image generation
 *   -> persist assistant reply
 * </pre>
 *
 * MAYA NEVER invents wardrobe items. The AI may propose IDs; Spring Boot is the
 * authority and re-validates each ID against the authenticated user's wardrobe.
 */
@Service
public class StylingService {

    private static final Logger log = LoggerFactory.getLogger(StylingService.class);

    private final WardrobeItemRepository wardrobeItemRepository;
    private final OutfitRepository outfitRepository;
    private final ConversationService conversationService;
    private final OutfitService outfitService;
    private final IntentExtractor intentExtractor;
    private final WeatherService weatherService;
    private final ImageStorageService imageStorageService;
    private final ChatAiClient chatAiClient;
    private final ImageGenerationClient imageGenerationClient;
    private final OutfitVisualizationService outfitVisualizationService;
    private final ShoppingSuggestionService shoppingSuggestionService;
    private final String visualizationMode;

    public StylingService(
            WardrobeItemRepository wardrobeItemRepository,
            OutfitRepository outfitRepository,
            ConversationService conversationService,
            OutfitService outfitService,
            IntentExtractor intentExtractor,
            WeatherService weatherService,
            ImageStorageService imageStorageService,
            ChatAiClient chatAiClient,
            ImageGenerationClient imageGenerationClient,
            OutfitVisualizationService outfitVisualizationService,
            ShoppingSuggestionService shoppingSuggestionService,
            @Value("${maya.visualization.mode:COMPOSITE}") String visualizationMode) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.outfitRepository = outfitRepository;
        this.conversationService = conversationService;
        this.outfitService = outfitService;
        this.intentExtractor = intentExtractor;
        this.weatherService = weatherService;
        this.imageStorageService = imageStorageService;
        this.chatAiClient = chatAiClient;
        this.imageGenerationClient = imageGenerationClient;
        this.outfitVisualizationService = outfitVisualizationService;
        this.shoppingSuggestionService = shoppingSuggestionService;
        this.visualizationMode = visualizationMode;
    }

    public ChatReplyResponse handleMessage(Long userId, Long conversationId, String message,
                                           Double latitude, Double longitude) {
        // 1. Persist the user's message
        conversationService.saveMessage(conversationId, MessageRole.USER, message);

        // 2. Extract intent deterministically
        StylingIntent intent = intentExtractor.extract(message);

        // 3. Weather / context (best-effort)
        WeatherInfo weather = weatherService.getWeather(latitude, longitude);

        // 4. Retrieve the user's actual wardrobe
        List<WardrobeItem> wardrobe = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (wardrobe == null || wardrobe.isEmpty()) {
            String reply = "You don't have any items in your wardrobe yet, so I can't style an outfit from your closet. "
                    + "Here are some pieces I'd suggest to build this look — or upload photos of your own clothes and I'll style those instead!";
            List<ShoppingSuggestion> suggestions = shoppingSuggestionService.suggest(message, intent);
            conversationService.saveMessage(conversationId, MessageRole.ASSISTANT, reply);
            return new ChatReplyResponse(reply, null, weather, suggestions);
        }

        // 5. Previous outfit in this conversation (for refinement / "another option")
        List<Long> previousItemIds = previousOutfitItemIds(conversationId);

        // 6. AI selection, then deterministic fallback, then ownership validation
        OutfitSelectionResponse aiSelection = chatAiClient.selectOutfit(wardrobe, intent, previousItemIds);
        OutfitSelectionResponse selection = validateSelection(userId, aiSelection);
        if (selection == null) {
            selection = selectDeterministic(wardrobe, intent, previousItemIds, weather);
        }

        // 7. Build and persist the outfit
        String hairstyle = selection.getHairstyle() != null ? selection.getHairstyle() : deterministicHairstyle(intent);
        String reason = selection.getReason() != null ? selection.getReason() : buildReason(selection, wardrobe);

        List<OutfitService.OutfitSelection> outfitSelections = selection.getItems().stream()
                .map(item -> new OutfitService.OutfitSelection(item.getWardrobeItemId(), item.getRole()))
                .collect(Collectors.toList());

        OutfitResponse outfitResponse = null;
        if (!outfitSelections.isEmpty()) {
            String generatedImagePath = generateVisualization(selection, hairstyle, intent, weather, wardrobe);
            outfitResponse = outfitService.createOutfit(userId, conversationId, hairstyle, reason, outfitSelections, generatedImagePath);
        }

        // 8. Compose and persist the assistant reply (offer "shop this" ideas when no look was possible)
        String reply;
        List<ShoppingSuggestion> suggestions = null;
        if (outfitResponse == null) {
            reply = "I couldn't build a complete look from your current wardrobe. Here are some pieces that would work — "
                    + "or upload more of your clothes and I'll style them.";
            suggestions = shoppingSuggestionService.suggest(message, intent);
        } else {
            reply = composeReply(outfitResponse, intent, weather);
        }
        conversationService.saveMessage(conversationId, MessageRole.ASSISTANT, reply);

        return new ChatReplyResponse(reply, outfitResponse, weather, suggestions);
    }

    private List<Long> previousOutfitItemIds(Long conversationId) {
        return outfitRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(Outfit::getItems)
                .map(items -> items.stream()
                        .map(OutfitItem::getWardrobeItemId)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    /**
     * Re-validates every AI-provided wardrobe ID: it must exist and belong to the
     * authenticated user, and its category must be compatible with the requested role.
     * Returns {@code null} if any item is invalid, forcing a deterministic fallback.
     */
    private OutfitSelectionResponse validateSelection(Long userId, OutfitSelectionResponse aiSelection) {
        if (aiSelection == null || aiSelection.getItems() == null || aiSelection.getItems().isEmpty()) {
            return null;
        }

        List<Long> ids = aiSelection.getItems().stream()
                .map(OutfitSelectionItem::getWardrobeItemId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return null;
        }

        Map<Long, WardrobeItem> owned = wardrobeItemRepository.findByUserIdAndIdIn(userId, ids).stream()
                .collect(Collectors.toMap(WardrobeItem::getId, item -> item));

        for (OutfitSelectionItem item : aiSelection.getItems()) {
            WardrobeItem wardrobeItem = owned.get(item.getWardrobeItemId());
            if (wardrobeItem == null) {
                log.warn("Rejecting outfit selection: wardrobe item {} not owned by user {}", item.getWardrobeItemId(), userId);
                return null;
            }
            if (!isRoleCompatible(item.getRole(), wardrobeItem.getCategory())) {
                log.warn("Rejecting outfit selection: item {} (category {}) incompatible with role {}",
                        item.getWardrobeItemId(), wardrobeItem.getCategory(), item.getRole());
                return null;
            }
        }

        return aiSelection;
    }

    private boolean isRoleCompatible(String role, ItemCategory category) {
        if (role == null || category == null) return false;
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "TOP" -> category == ItemCategory.TOP;
            case "BOTTOM" -> category == ItemCategory.BOTTOM;
            case "DRESS" -> category == ItemCategory.DRESS;
            case "OUTERWEAR" -> category == ItemCategory.OUTERWEAR;
            case "SHOES" -> category == ItemCategory.SHOES;
            case "BAG" -> category == ItemCategory.BAG;
            case "ACCESSORY" -> category == ItemCategory.ACCESSORY || category == ItemCategory.JEWELRY;
            case "JEWELRY" -> category == ItemCategory.JEWELRY || category == ItemCategory.ACCESSORY;
            default -> false;
        };
    }

    private OutfitSelectionResponse selectDeterministic(List<WardrobeItem> wardrobe, StylingIntent intent,
                                                        List<Long> previousItemIds, WeatherInfo weather) {
        List<WardrobeItem> available = wardrobe.stream()
                .filter(item -> !isExcluded(item, intent))
                .filter(item -> !previousItemIds.contains(item.getId()))
                .collect(Collectors.toList());

        // If "another option" plus exclusions emptied the pool, relax to only exclusions.
        if (available.isEmpty()) {
            available = wardrobe.stream().filter(item -> !isExcluded(item, intent)).collect(Collectors.toList());
        }

        Map<ItemCategory, List<WardrobeItem>> byCategory = available.stream()
                .collect(Collectors.groupingBy(WardrobeItem::getCategory));

        List<Chosen> chosen = new ArrayList<>();

        if (shouldUseDress(intent, byCategory)) {
            pick(byCategory, ItemCategory.DRESS, "DRESS", intent, chosen);
        } else {
            pick(byCategory, ItemCategory.TOP, "TOP", intent, chosen);
            pick(byCategory, ItemCategory.BOTTOM, "BOTTOM", intent, chosen);
        }

        if (includeOuterwear(intent, weather, byCategory.containsKey(ItemCategory.DRESS) && chosen.size() == 1 && chosen.get(0).role().equals("DRESS"))) {
            pick(byCategory, ItemCategory.OUTERWEAR, "OUTERWEAR", intent, chosen);
        }
        pick(byCategory, ItemCategory.SHOES, "SHOES", intent, chosen);
        pick(byCategory, ItemCategory.BAG, "BAG", intent, chosen);
        if (!pick(byCategory, ItemCategory.JEWELRY, "JEWELRY", intent, chosen)) {
            pick(byCategory, ItemCategory.ACCESSORY, "ACCESSORY", intent, chosen);
        }

        if (chosen.isEmpty()) {
            return new OutfitSelectionResponse(List.of(), null, null);
        }

        List<OutfitSelectionItem> items = chosen.stream()
                .map(c -> new OutfitSelectionItem(c.item().getId(), c.role()))
                .collect(Collectors.toList());

        String reason = buildReasonFromChosen(chosen, intent);
        return new OutfitSelectionResponse(items, reason, deterministicHairstyle(intent));
    }

    private boolean pick(Map<ItemCategory, List<WardrobeItem>> byCategory, ItemCategory category,
                         String role, StylingIntent intent, List<Chosen> chosen) {
        List<WardrobeItem> list = byCategory.get(category);
        if (list == null || list.isEmpty()) {
            return false;
        }
        WardrobeItem best = list.stream()
                .max(Comparator.comparingInt((WardrobeItem i) -> score(i, intent))
                        .thenComparing((a, b) -> Long.compare(b.getId(), a.getId())))
                .orElse(null);
        if (best != null) {
            chosen.add(new Chosen(best, role));
            return true;
        }
        return false;
    }

    private boolean shouldUseDress(StylingIntent intent, Map<ItemCategory, List<WardrobeItem>> byCategory) {
        boolean hasDress = byCategory.containsKey(ItemCategory.DRESS);
        if (!hasDress) return false;

        String style = intent.getStyle();
        if (intent.isMoreFeminine() || "feminine".equals(style) || "cute".equals(style) || "elegant".equals(style)) {
            return true;
        }
        String occasion = intent.getOccasion();
        if (occasion != null && Set.of("brunch", "date", "party", "wedding", "dinner").contains(occasion)) {
            return true;
        }
        return !byCategory.containsKey(ItemCategory.TOP) || !byCategory.containsKey(ItemCategory.BOTTOM);
    }

    private boolean includeOuterwear(StylingIntent intent, WeatherInfo weather, boolean wearingDress) {
        if ("sporty".equals(intent.getStyle()) || "beach".equals(intent.getOccasion()) || "gym".equals(intent.getOccasion())) {
            return false;
        }
        if (wearingDress && "formal".equals(intent.getFormality())) {
            return false;
        }
        // Layer when it is cool; still offer a light layer in warm weather if casual.
        if (weather != null && weather.getTemperature() != null && weather.getTemperature() > 26.0) {
            return false;
        }
        return true;
    }

    private boolean isExcluded(WardrobeItem item, StylingIntent intent) {
        String haystack = lower(item.getDescription() + " " + item.getSubcategory() + " " + item.getCategory());
        for (String exclusion : intent.getExclusions()) {
            if (haystack.contains(lower(exclusion))) {
                return true;
            }
        }
        return false;
    }

    private int score(WardrobeItem item, StylingIntent intent) {
        int s = 0;
        String style = lower(item.getStyle());
        String color = lower(item.getColor());
        String formality = lower(item.getFormality());

        if (intent.getStyle() != null && style.contains(intent.getStyle())) {
            s += 2;
        }
        if (intent.isMoreFeminine()) {
            if (containsAny(style, "feminine", "elegant", "chic", "romantic")) s += 2;
            if (containsAny(color, "pink", "blush", "rose", "cream", "ivory", "lilac", "pastel", "beige")) s += 1;
        }
        if (intent.isMoreCasual()) {
            if (containsAny(style, "casual", "sporty", "cozy", "relaxed")) s += 2;
            if (containsAny(formality, "casual")) s += 1;
        }
        if (intent.isMoreFormal() || "formal".equals(intent.getFormality())) {
            if (containsAny(formality, "formal", "evening", "business")) s += 2;
            if (containsAny(style, "elegant", "chic", "formal")) s += 1;
        }
        if ("casual".equals(intent.getFormality()) && containsAny(formality, "casual")) {
            s += 1;
        }
        return s;
    }

    private String buildReasonFromChosen(List<Chosen> chosen, StylingIntent intent) {
        if (chosen.size() == 1) {
            return "This " + chosen.get(0).item().getDescription() + " works on its own and fits your requested " +
                    (intent.getStyle() != null ? intent.getStyle() : "style") + " aesthetic.";
        }
        StringBuilder sb = new StringBuilder();
        Chosen first = chosen.get(0);
        sb.append("This works because the ").append(first.item().getDescription());
        if (chosen.size() > 1) {
            sb.append(" pairs well with the ").append(chosen.get(1).item().getDescription());
        }
        sb.append(", while the overall look stays balanced and appropriate for the occasion.");
        return sb.toString();
    }

    private String buildReason(OutfitSelectionResponse selection, List<WardrobeItem> wardrobe) {
        Map<Long, WardrobeItem> byId = wardrobe.stream()
                .collect(Collectors.toMap(WardrobeItem::getId, item -> item));
        List<String> descriptions = selection.getItems().stream()
                .map(item -> byId.get(item.getWardrobeItemId()))
                .filter(java.util.Objects::nonNull)
                .map(WardrobeItem::getDescription)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (descriptions.isEmpty()) {
            return "A balanced look curated from your wardrobe.";
        }
        if (descriptions.size() == 1) {
            return "This " + descriptions.get(0) + " works on its own for the occasion.";
        }
        return "This works because the " + descriptions.get(0) + " pairs well with the " + descriptions.get(1) +
                ", keeping the whole look cohesive.";
    }

    private String deterministicHairstyle(StylingIntent intent) {
        if (intent.isMoreFeminine() || "feminine".equals(intent.getStyle())) {
            return "Half-up hairstyle with loose front strands";
        }
        if (intent.isMoreFormal() || "formal".equals(intent.getFormality())) {
            return "Elegant low chignon with a soft side part";
        }
        if (intent.isMoreCasual() || "casual".equals(intent.getFormality())) {
            return "Effortless loose waves or a low ponytail";
        }
        return "Soft, natural waves with a middle part";
    }

    /**
     * Builds the outfit visualization. By default this is a deterministic "look board"
     * composed from the user's ACTUAL wardrobe item images — never an AI-invented picture.
     * Set {@code maya.visualization.mode=AI} to use the configured image-generation provider.
     */
    private String generateVisualization(OutfitSelectionResponse selection, String hairstyle,
                                         StylingIntent intent, WeatherInfo weather, List<WardrobeItem> wardrobe) {
        try {
            if ("AI".equalsIgnoreCase(visualizationMode)) {
                return generateAiVisualization(selection, hairstyle, intent, weather, wardrobe);
            }
            return generateCompositeVisualization(selection, wardrobe);
        } catch (Exception e) {
            log.warn("Outfit visualization skipped: {}", e.getMessage());
            return null;
        }
    }

    private String generateCompositeVisualization(OutfitSelectionResponse selection, List<WardrobeItem> wardrobe) {
        Map<Long, WardrobeItem> byId = wardrobe.stream()
                .collect(Collectors.toMap(WardrobeItem::getId, item -> item));

        List<OutfitVisualizationService.Piece> pieces = selection.getItems().stream()
                .map(item -> {
                    WardrobeItem wardrobeItem = byId.get(item.getWardrobeItemId());
                    if (wardrobeItem == null || wardrobeItem.getImagePath() == null) {
                        return null;
                    }
                    return new OutfitVisualizationService.Piece(wardrobeItem.getImagePath(), item.getRole());
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        byte[] board = outfitVisualizationService.composeBoard(pieces);
        if (board == null || board.length == 0) {
            return null;
        }
        return imageStorageService.store(new ByteArrayInputStream(board), "lookboard.png", "outfits");
    }

    private String generateAiVisualization(OutfitSelectionResponse selection, String hairstyle,
                                           StylingIntent intent, WeatherInfo weather, List<WardrobeItem> wardrobe) {
        String prompt = buildImagePrompt(selection, hairstyle, intent, weather, wardrobe);
        byte[] imageBytes = imageGenerationClient.generateImage(prompt);
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        String filename = "outfit." + detectImageExtension(imageBytes);
        return imageStorageService.store(new ByteArrayInputStream(imageBytes), filename, "outfits");
    }

    /**
     * Detects the real image format from magic bytes so the stored file extension and
     * served content type match the actual bytes (image providers may return JPEG or PNG).
     */
    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "png";
        }
        if (bytes.length >= 12 && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return "png";
    }

    private String buildImagePrompt(OutfitSelectionResponse selection, String hairstyle,
                                    StylingIntent intent, WeatherInfo weather, List<WardrobeItem> wardrobe) {
        Map<Long, WardrobeItem> byId = wardrobe.stream()
                .collect(Collectors.toMap(WardrobeItem::getId, item -> item));

        // Describe the actual selected garment metadata when available, falling back to the role.
        List<String> itemDescriptions = selection.getItems().stream()
                .map(item -> {
                    WardrobeItem wardrobeItem = byId.get(item.getWardrobeItemId());
                    if (wardrobeItem == null || wardrobeItem.getDescription() == null) {
                        return item.getRole().toLowerCase(Locale.ROOT);
                    }
                    String color = wardrobeItem.getColor() != null ? wardrobeItem.getColor() + " " : "";
                    return color + wardrobeItem.getDescription();
                })
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("Fashion lookbook photo of a person wearing a complete outfit: ");
        sb.append(String.join(", ", itemDescriptions));
        sb.append(". Hairstyle: ").append(hairstyle).append(".");
        if (intent.getOccasion() != null) sb.append(" Occasion: ").append(intent.getOccasion()).append(".");
        if (intent.getStyle() != null) sb.append(" Style: ").append(intent.getStyle()).append(".");
        if (weather != null && weather.getCondition() != null) {
            sb.append(" Weather: ").append(weather.getCondition()).append(".");
        }
        sb.append(" Full-length, photorealistic, neutral studio background.");
        return sb.toString();
    }

    private String composeReply(OutfitResponse outfit, StylingIntent intent, WeatherInfo weather) {
        if (outfit == null || outfit.getItems() == null || outfit.getItems().isEmpty()) {
            return "I couldn't put together a full look from your current wardrobe. Upload a few more pieces and I'll try again!";
        }
        StringBuilder sb = new StringBuilder();
        if (intent.getOccasion() != null) {
            sb.append("Here's a ").append(intent.getOccasion()).append(" look I put together for you!");
        } else {
            sb.append("Here's a look I put together for you!");
        }
        if (weather != null && weather.getCondition() != null) {
            sb.append(" (I considered today's ").append(weather.getCondition().toLowerCase(Locale.ROOT)).append(" weather.)");
        }
        return sb.toString();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String haystack, String... tokens) {
        if (haystack == null) return false;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private record Chosen(WardrobeItem item, String role) {
    }
}
