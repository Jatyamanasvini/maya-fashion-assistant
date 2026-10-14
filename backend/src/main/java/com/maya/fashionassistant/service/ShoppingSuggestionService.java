package com.maya.fashionassistant.service;

import com.maya.fashionassistant.ai.ChatAiClient;
import com.maya.fashionassistant.dto.ai.ShoppingIdea;
import com.maya.fashionassistant.dto.shopping.ShoppingSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds "shop this" suggestions for when the user's wardrobe cannot fulfil a request.
 *
 * <p>These are explicitly <b>not</b> owned items. The AI proposes pieces to buy; a free,
 * keyless image search (Openverse) finds a real photo for each. If no photo is found, a
 * generated illustration URL is used as a fallback.</p>
 */
@Service
public class ShoppingSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingSuggestionService.class);

    private final ChatAiClient chatAiClient;
    private final ImageSearchService imageSearchService;

    public ShoppingSuggestionService(ChatAiClient chatAiClient, ImageSearchService imageSearchService) {
        this.chatAiClient = chatAiClient;
        this.imageSearchService = imageSearchService;
    }

    private static final int MAX_SUGGESTIONS = 4;

    public List<ShoppingSuggestion> suggest(String userMessage, StylingIntent intent) {
        List<ShoppingIdea> ideas = chatAiClient.suggestShoppingIdeas(userMessage, intent);
        List<ShoppingSuggestion> suggestions = new ArrayList<>();
        if (ideas == null) {
            return suggestions;
        }

        for (ShoppingIdea idea : ideas) {
            if (suggestions.size() >= MAX_SUGGESTIONS) {
                break;
            }
            if (idea.getTitle() == null || idea.getTitle().isBlank()) {
                continue;
            }

            String imageUrl = null;
            String sourceUrl = null;
            if (idea.getQuery() != null && !idea.getQuery().isBlank()) {
                ImageSearchService.SearchResult result = imageSearchService.findImage(idea.getQuery());
                if (result != null) {
                    imageUrl = result.imageUrl();
                    sourceUrl = result.sourceUrl();
                }
            }
            if (imageUrl == null) {
                imageUrl = fallbackImageUrl(idea.getTitle());
            }

            suggestions.add(new ShoppingSuggestion(
                    idea.getCategory(), idea.getTitle(), idea.getReason(), imageUrl, sourceUrl));
        }
        return suggestions;
    }

    /** Free, keyless generated illustration as a fallback for the suggestion image. */
    private String fallbackImageUrl(String title) {
        String prompt = URLEncoder.encode("professional product photo of " + title + ", white background",
                StandardCharsets.UTF_8).replace("+", "%20");
        return "https://image.pollinations.ai/prompt/" + prompt + "?width=512&height=512&nologo=true";
    }
}
