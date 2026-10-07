package com.maya.fashionassistant.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic, rule-based intent extraction for styling requests.
 *
 * <p>This handles the common conversational refinements ("I don't want sneakers",
 * "make it more feminine", "give me another option") and the frequent occasion/style
 * keywords, so that MAYA works correctly even without an AI provider configured.
 * The AI is used for ranking and reasoning, not for authorization or constraint logic.</p>
 */
@Component
public class IntentExtractor {

    private static final Map<String, String> OCCASIONS = Map.ofEntries(
            Map.entry("brunch", "brunch"),
            Map.entry("breakfast", "brunch"),
            Map.entry("lunch", "casual"),
            Map.entry("dinner", "dinner"),
            Map.entry("supper", "dinner"),
            Map.entry("wedding", "wedding"),
            Map.entry("date", "date"),
            Map.entry("party", "party"),
            Map.entry("club", "party"),
            Map.entry("work", "work"),
            Map.entry("office", "work"),
            Map.entry("interview", "interview"),
            Map.entry("meeting", "work"),
            Map.entry("beach", "beach"),
            Map.entry("gym", "gym"),
            Map.entry("travel", "travel"),
            Map.entry("formal", "formal"),
            Map.entry("church", "formal"),
            Map.entry("graduation", "formal")
    );

    private static final Map<String, String> STYLES = Map.ofEntries(
            Map.entry("cute", "cute"),
            Map.entry("feminine", "feminine"),
            Map.entry("girly", "feminine"),
            Map.entry("elegant", "elegant"),
            Map.entry("chic", "chic"),
            Map.entry("cozy", "cozy"),
            Map.entry("comfy", "cozy"),
            Map.entry("comfortable", "cozy"),
            Map.entry("casual", "casual"),
            Map.entry("sporty", "sporty"),
            Map.entry("athletic", "sporty"),
            Map.entry("minimal", "minimal"),
            Map.entry("minimalist", "minimal"),
            Map.entry("edgy", "edgy"),
            Map.entry("boho", "boho"),
            Map.entry("classy", "elegant"),
            Map.entry("sexy", "elegant")
    );

    // Wardrobe categories / subcategories that map to the "I don't want X" pattern.
    private static final List<String> CATEGORY_KEYWORDS = List.of(
            "sneakers", "sneaker", "trainers", "shoes", "heels", "boots", "sandals",
            "bag", "purse", "handbag", "clutch",
            "jacket", "coat", "blazer", "cardigan",
            "jeans", "pants", "trousers", "skirt", "shorts",
            "shirt", "top", "blouse", "tshirt", "t-shirt",
            "dress", "jewelry", "necklace", "earrings", "hat", "scarf"
    );

    public StylingIntent extract(String rawMessage) {
        StylingIntent intent = new StylingIntent();
        if (rawMessage == null) {
            return intent;
        }

        String message = rawMessage.toLowerCase(Locale.ROOT).trim();

        // Refinement signals
        if (containsAny(message, "more feminine", "more girly", "make it feminine", "make it more feminine")) {
            intent.setMoreFeminine(true);
        }
        if (containsAny(message, "more casual", "make it casual", "dress down", "less formal")) {
            intent.setMoreCasual(true);
        }
        if (containsAny(message, "more formal", "dress up", "make it formal", "more elegant")) {
            intent.setMoreFormal(true);
        }
        if (containsAny(message, "another option", "another look", "different option", "something else",
                "give me another", "try again", "one more", "again")) {
            intent.setAnotherOption(true);
        }

        // Negative constraints: "I don't want sneakers", "no heels", "without a jacket"
        extractExclusions(message, intent);

        // Occasion detection (first match wins on a best-effort basis)
        for (Map.Entry<String, String> entry : OCCASIONS.entrySet()) {
            if (message.contains(entry.getKey())) {
                intent.setOccasion(entry.getValue());
                break;
            }
        }

        // Style detection
        for (Map.Entry<String, String> entry : STYLES.entrySet()) {
            if (message.contains(entry.getKey())) {
                intent.setStyle(entry.getValue());
                break;
            }
        }

        // Formality derivation
        if (containsAny(message, "formal", "business", "black tie", "gala", "wedding", "interview")) {
            intent.setFormality("formal");
        } else if (containsAny(message, "smart casual", "smart-casual", "business casual")) {
            intent.setFormality("smart_casual");
        } else if (containsAny(message, "casual", "comfy", "cozy", "chill", "relaxed")) {
            intent.setFormality("casual");
        }

        // Occasion-based formality default when not explicitly stated
        if (intent.getFormality() == null && intent.getOccasion() != null) {
            intent.setFormality(defaultFormality(intent.getOccasion()));
        }

        return intent;
    }

    private String defaultFormality(String occasion) {
        return switch (occasion) {
            case "brunch", "casual", "beach", "gym", "travel" -> "casual";
            case "dinner", "date", "party" -> "smart_casual";
            case "work", "interview" -> "business";
            case "wedding", "formal", "graduation" -> "formal";
            default -> "smart_casual";
        };
    }

    private void extractExclusions(String message, StylingIntent intent) {
        for (String keyword : CATEGORY_KEYWORDS) {
            if (hasNegation(message, keyword)) {
                intent.addExclusion(keyword);
            }
        }
    }

    private boolean hasNegation(String message, String keyword) {
        // Patterns: "no sneakers", "don't want sneakers", "without sneakers", "not sneakers", "remove the sneakers"
        return message.contains("no " + keyword)
                || message.contains("don't want " + keyword)
                || message.contains("dont want " + keyword)
                || message.contains("do not want " + keyword)
                || message.contains("without " + keyword)
                || message.contains("not " + keyword)
                || message.contains("remove the " + keyword)
                || message.contains("drop the " + keyword)
                || message.contains("no more " + keyword);
    }

    private boolean containsAny(String message, String... tokens) {
        for (String token : tokens) {
            if (message.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
