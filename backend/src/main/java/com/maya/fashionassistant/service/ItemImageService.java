package com.maya.fashionassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Produces an isolated "product shot" image for a single wardrobe item: the garment
 * cropped from the source photo, background removed, centered on white.
 *
 * <p>Two execution modes:</p>
 * <ul>
 *   <li><b>Remote</b> — POSTs to a segmentation microservice when
 *       {@code maya.segmentation.remote-url} is set. This lets a memory-constrained
 *       host (e.g. Render's free 512 MB tier) run the JVM alone while the heavy Python
 *       inference runs in a separate container.</li>
 *   <li><b>Local</b> — runs {@code scripts/crop_item.py} as a subprocess when
 *       {@code maya.segmentation.enabled=true} and Python is installed.</li>
 * </ul>
 * Returns {@code null} on any failure so the caller can fall back to the original photo.
 */
@Service
public class ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageService.class);

    private final ImageStorageService imageStorageService;
    private final boolean enabled;
    private final String remoteUrl;
    private final String pythonExecutable;
    private final String scriptPath;
    private final RestClient restClient = RestClient.create();

    public ItemImageService(
            ImageStorageService imageStorageService,
            @Value("${maya.segmentation.enabled:true}") boolean enabled,
            @Value("${maya.segmentation.remote-url:}") String remoteUrl,
            @Value("${maya.segmentation.python:python3.11}") String pythonExecutable,
            @Value("${maya.segmentation.script:scripts/crop_item.py}") String scriptPath) {
        this.imageStorageService = imageStorageService;
        this.enabled = enabled;
        this.remoteUrl = remoteUrl;
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
    }

    public String createItemImage(String sourceStoragePath, List<Double> bbox, String category) {
        boolean useRemote = StringUtils.hasText(remoteUrl);
        if (!useRemote && !enabled) {
            return null;
        }

        boolean clothCategory = category != null
                && Set.of("TOP", "BOTTOM", "DRESS", "OUTERWEAR").contains(category.toUpperCase());
        boolean hasBbox = bbox != null && bbox.size() == 4;
        if (!hasBbox && !clothCategory) {
            return null;
        }

        if (useRemote) {
            String remote = createViaRemote(sourceStoragePath, bbox, category);
            if (remote != null) {
                return remote;
            }
            log.warn("Remote segmentation unavailable; falling back to local if enabled");
        }
        return enabled ? createViaLocalShell(sourceStoragePath, bbox, category) : null;
    }

    private String createViaRemote(String sourceStoragePath, List<Double> bbox, String category) {
        try {
            Resource source = imageStorageService.loadAsResource(sourceStoragePath);
            byte[] bytes;
            try (InputStream in = source.getInputStream()) {
                bytes = in.readAllBytes();
            }

            ByteArrayResource file = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return "image.jpg";
                }
            };

            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("image", file);
            form.add("bbox", bbox != null && bbox.size() == 4
                    ? bbox.stream().map(String::valueOf).collect(Collectors.joining(","))
                    : "-");
            form.add("category", category != null ? category.toUpperCase() : "OTHER");

            String base = remoteUrl.startsWith("http") ? remoteUrl : "https://" + remoteUrl;
            base = base.replaceAll("/+$", "");

            byte[] out = restClient.post()
                    .uri(base + "/segment")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(byte[].class);

            if (out == null || out.length == 0) {
                return null;
            }
            try (InputStream in = new ByteArrayInputStream(out)) {
                return imageStorageService.store(in, "item.png", "items");
            }
        } catch (Exception e) {
            log.warn("Remote segmentation failed: {}", e.getMessage());
            return null;
        }
    }

    private String createViaLocalShell(String sourceStoragePath, List<Double> bbox, String category) {
        Path tempOutput = null;
        try {
            Resource source = imageStorageService.loadAsResource(sourceStoragePath);
            File sourceFile = source.getFile();
            if (!sourceFile.exists()) {
                return null;
            }

            String bboxArg = bbox != null && bbox.size() == 4
                    ? bbox.stream().map(String::valueOf).collect(Collectors.joining(","))
                    : "-";
            String categoryArg = category != null ? category.toUpperCase() : "OTHER";
            tempOutput = Files.createTempFile("maya-item-", ".png");

            ProcessBuilder builder = new ProcessBuilder(
                    pythonExecutable, scriptPath, sourceFile.getAbsolutePath(), bboxArg, categoryArg,
                    tempOutput.toString());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("Item segmentation timed out");
                return null;
            }
            if (process.exitValue() != 0 || !Files.exists(tempOutput) || Files.size(tempOutput) == 0) {
                log.warn("Item segmentation failed (exit {}): {}", process.exitValue(), output.trim());
                return null;
            }

            try (InputStream in = Files.newInputStream(tempOutput)) {
                return imageStorageService.store(in, "item.png", "items");
            }
        } catch (Exception e) {
            log.warn("Item image creation failed: {}", e.getMessage());
            return null;
        } finally {
            if (tempOutput != null) {
                try {
                    Files.deleteIfExists(tempOutput);
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }
}
