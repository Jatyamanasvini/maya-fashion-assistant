package com.maya.fashionassistant.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public diagnostics for deployment troubleshooting. Reports the active configuration
 * (with secrets masked) and can probe the AI provider and the segmentation service so the
 * exact remote error is visible in a browser — no log hunting required.
 *
 * <p>It never returns the API key. Disable/secure these endpoints for a real production app.</p>
 */
@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsController.class);

    private final RestClient restClient;
    private final RestClient segmentationClient;
    private final Environment environment;
    private final String baseUrl;
    private final String apiKey;
    private final String provider;
    private final String chatModel;
    private final String visionModel;
    private final String fallbackModels;
    private final boolean segmentationEnabled;
    private final String segmentationRemoteUrl;
    private final String datasourceUrl;

    public DiagnosticsController(
            Environment environment,
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.provider:MOCK}") String provider,
            @Value("${ai.model:gpt-4o-mini}") String chatModel,
            @Value("${ai.vision-model:gpt-4o-mini}") String visionModel,
            @Value("${ai.fallback-models:}") String fallbackModels,
            @Value("${maya.segmentation.enabled:true}") boolean segmentationEnabled,
            @Value("${maya.segmentation.remote-url:}") String segmentationRemoteUrl,
            @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.environment = environment;
        this.datasourceUrl = datasourceUrl;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.provider = provider;
        this.chatModel = chatModel;
        this.visionModel = visionModel;
        this.fallbackModels = fallbackModels;
        this.segmentationEnabled = segmentationEnabled;
        this.segmentationRemoteUrl = segmentationRemoteUrl;

        // Bounded timeouts: an unreachable provider must never hang the whole request.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.segmentationClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /** Quick, AI-free view of the running configuration (handy to confirm a deploy went live). */
    @GetMapping("/build")
    public Map<String, Object> build() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("app", "maya-fashion-assistant");
        out.put("profiles", String.join(",", environment.getActiveProfiles()));
        out.put("segmentationEnabled", segmentationEnabled);
        out.put("segmentationRemote", StringUtils.hasText(segmentationRemoteUrl) ? segmentationRemoteUrl : null);
        out.put("aiProvider", provider);
        out.put("chatModel", chatModel);
        out.put("visionModel", visionModel);
        out.put("apiKeyPresent", StringUtils.hasText(apiKey));
        out.put("database", describeDatabase());
        out.put("time", Instant.now().toString());
        return out;
    }

    /** Live probe of the cutout service: proves the app can actually reach it. */
    @GetMapping("/segmentation")
    public Map<String, Object> segmentation() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("remoteUrl", StringUtils.hasText(segmentationRemoteUrl) ? segmentationRemoteUrl : null);
        if (!StringUtils.hasText(segmentationRemoteUrl)) {
            out.put("ok", false);
            out.put("error", "MAYA_SEGMENTATION_REMOTE_URL is not configured");
            return out;
        }
        String base = segmentationRemoteUrl.startsWith("http")
                ? segmentationRemoteUrl
                : "https://" + segmentationRemoteUrl;
        base = base.replaceAll("/+$", "");
        try {
            String body = segmentationClient.get()
                    .uri(base + "/health")
                    .retrieve()
                    .body(String.class);
            out.put("ok", true);
            out.put("health", body);
        } catch (RestClientResponseException e) {
            out.put("ok", false);
            out.put("status", e.getStatusCode().value());
            out.put("error", truncate(e.getResponseBodyAsString(), 500));
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
        }
        return out;
    }

    @GetMapping("/ai")
    public Map<String, Object> ai(
            @RequestParam(value = "all", required = false, defaultValue = "false") boolean all) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("provider", provider);
        out.put("baseUrl", baseUrl);
        out.put("chatModel", chatModel);
        out.put("visionModel", visionModel);
        out.put("fallbackModels", fallbackModels);
        out.put("segmentationEnabled", segmentationEnabled);
        out.put("segmentationRemote", StringUtils.hasText(segmentationRemoteUrl) ? segmentationRemoteUrl : null);
        out.put("apiKeyPresent", StringUtils.hasText(apiKey));
        out.put("apiKeyHint", StringUtils.hasText(apiKey)
                ? apiKey.substring(0, Math.min(6, apiKey.length())) + "…(len=" + apiKey.length() + ")"
                : null);
        out.put("hint", "OpenAI/OPENAI provider needs a key; MOCK ignores AI and returns samples.");

        List<Map<String, Object>> probes = new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(chatModel)) candidates.add(chatModel.trim());
        if (StringUtils.hasText(fallbackModels)) {
            for (String m : fallbackModels.split(",")) {
                if (StringUtils.hasText(m) && !candidates.contains(m.trim())) candidates.add(m.trim());
            }
        }
        if (!StringUtils.hasText(apiKey)) {
            out.put("probes", probes);
            out.put("note", "No API key configured — the app is running in MOCK mode.");
            return out;
        }
        List<String> toProbe = all ? candidates : candidates.subList(0, Math.min(1, candidates.size()));
        for (String candidate : toProbe) {
            probes.add(probe(candidate));
        }
        out.put("probes", probes);
        if (!all) {
            out.put("note", "Probing only the primary model. Add ?all=true to test every fallback (uses more free-tier quota).");
        }
        return out;
    }

    private Map<String, Object> probe(String model) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", model);
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", "Reply with JSON: {\"ok\":true}")),
                    "response_format", Map.of("type", "json_object"));
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            result.put("ok", true);
            result.put("sample", truncate(response, 200));
        } catch (RestClientResponseException e) {
            result.put("ok", false);
            result.put("status", e.getStatusCode().value());
            result.put("error", truncate(e.getResponseBodyAsString(), 500));
        } catch (Exception e) {
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        log.info("AI diagnostic probe {} -> {}", model, result.get("ok"));
        return result;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    /** Human-readable database target with credentials masked (never exposes a password). */
    private String describeDatabase() {
        if (!StringUtils.hasText(datasourceUrl)) {
            return "unknown";
        }
        String masked = datasourceUrl.replaceAll("//[^/@]+@", "//***@");
        String lower = datasourceUrl.toLowerCase();
        String type;
        if (lower.startsWith("jdbc:h2")) {
            type = "H2 (EPHEMERAL — wiped on restart)";
        } else if (lower.startsWith("jdbc:mysql")) {
            type = "MySQL (persistent)";
        } else if (lower.startsWith("jdbc:postgresql")) {
            type = "PostgreSQL (persistent)";
        } else {
            type = "other";
        }
        return type + " | " + masked;
    }
}
