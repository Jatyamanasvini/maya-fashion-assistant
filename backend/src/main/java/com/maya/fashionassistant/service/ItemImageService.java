package com.maya.fashionassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Produces an isolated "product shot" image for a single wardrobe item: the garment
 * cropped from the source photo using the model-provided bounding box, background
 * removed (GrabCut), and centered on a plain white background.
 *
 * <p>Segmentation runs in a small local OpenCV script ({@code scripts/crop_item.py}).
 * If it is unavailable or fails, this returns {@code null} and the caller falls back
 * to the original photo.</p>
 */
@Service
public class ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageService.class);

    private final ImageStorageService imageStorageService;
    private final boolean enabled;
    private final String pythonExecutable;
    private final String scriptPath;

    public ItemImageService(
            ImageStorageService imageStorageService,
            @Value("${maya.segmentation.enabled:true}") boolean enabled,
            @Value("${maya.segmentation.python:python3.11}") String pythonExecutable,
            @Value("${maya.segmentation.script:scripts/crop_item.py}") String scriptPath) {
        this.imageStorageService = imageStorageService;
        this.enabled = enabled;
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
    }

    /**
     * @param sourceStoragePath stored path of the original uploaded photo
     * @param bbox              normalized [x1,y1,x2,y2] fractions of the image
     * @return stored path of the isolated item image, or {@code null} on failure
     */
    public String createItemImage(String sourceStoragePath, List<Double> bbox) {
        if (!enabled || bbox == null || bbox.size() != 4) {
            return null;
        }

        Path tempOutput = null;
        try {
            Resource source = imageStorageService.loadAsResource(sourceStoragePath);
            File sourceFile = source.getFile();
            if (!sourceFile.exists()) {
                return null;
            }

            String bboxArg = bbox.stream().map(String::valueOf).collect(Collectors.joining(","));
            tempOutput = Files.createTempFile("maya-item-", ".png");

            ProcessBuilder builder = new ProcessBuilder(
                    pythonExecutable, scriptPath, sourceFile.getAbsolutePath(), bboxArg, tempOutput.toString());
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
                    // best effort cleanup
                }
            }
        }
    }
}
