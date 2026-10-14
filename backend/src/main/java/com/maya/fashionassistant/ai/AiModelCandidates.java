package com.maya.fashionassistant.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Small helpers shared by the OpenAI-compatible AI clients.
 *
 * <p>AI providers rename/deprecate models frequently (e.g. Google's Gemini 2.5 models
 * became access-restricted for new keys). To stay resilient we try a configured model
 * followed by a configurable list of fallbacks, tolerate providers that reject JSON mode,
 * and clean up responses that arrive wrapped in markdown.</p>
 */
final class AiModelCandidates {

    private AiModelCandidates() {
    }

    /** Ordered, de-duplicated list of models to try: primary first, then fallbacks. */
    static List<String> build(String primary, String fallbacks) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (primary != null && !primary.isBlank()) {
            ordered.add(primary.trim());
        }
        if (fallbacks != null) {
            for (String candidate : fallbacks.split(",")) {
                if (!candidate.isBlank()) {
                    ordered.add(candidate.trim());
                }
            }
        }
        return new ArrayList<>(ordered);
    }

    /** Heuristic: did the provider reject our {@code response_format: json_object}? */
    static boolean looksLikeFormatError(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        String body = responseBody.toLowerCase();
        return body.contains("response_format")
                || body.contains("responseformat")
                || body.contains("json_object")
                || body.contains("json mode")
                || (body.contains("format") && body.contains("support"));
    }

    /** Extracts a JSON object from a model response, stripping markdown code fences. */
    static String extractJson(String content) {
        if (content == null) {
            return "";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int closingFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && closingFence > firstNewline) {
                text = text.substring(firstNewline + 1, closingFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text;
    }
}
