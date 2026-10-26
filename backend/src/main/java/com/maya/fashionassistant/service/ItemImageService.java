package com.maya.fashionassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Produces an isolated "product shot" image for a single wardrobe item.
 *
 * <p>Execution chain (first success wins):</p>
 * <ol>
 *   <li><b>Remote segmenter</b> — POSTs to the segmentation microservice when
 *       {@code maya.segmentation.remote-url} is set (background removed, on white).</li>
 *   <li><b>Local Python</b> — runs {@code scripts/crop_item.py} when
 *       {@code maya.segmentation.enabled=true} and Python is installed.</li>
 *   <li><b>Java fallback</b> — always available: crops the AI-supplied bounding box and
 *       centres it on a white square. Not a true cutout, but guarantees the item gets a
 *       clean "product style" thumbnail instead of the whole group photo.</li>
 * </ol>
 * The caller uses the returned storage path; a {@code null} result means "keep original".
 */
@Service
public class ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageService.class);

    private final ImageStorageService imageStorageService;
    private final boolean enabled;
    private final String remoteUrl;
    private final String pythonExecutable;
    private final String scriptPath;
    private final RestClient restClient;

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

        // Free hosting sleeps, so cold starts are slow: be generous, but bounded.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        log.info("Item image service: remote segmentation {} (url={}), local python {}",
                StringUtils.hasText(remoteUrl) ? "ENABLED" : "disabled",
                StringUtils.hasText(remoteUrl) ? remoteUrl : "-",
                enabled ? "enabled" : "disabled");
    }

    public String createItemImage(String sourceStoragePath, List<Double> bbox, String category) {
        boolean useRemote = StringUtils.hasText(remoteUrl);
        boolean hasBbox = bbox != null && bbox.size() == 4;
        boolean clothCategory = category != null
                && Set.of("TOP", "BOTTOM", "DRESS", "OUTERWEAR").contains(category.toUpperCase());

        if (!hasBbox && !clothCategory) {
            return null;
        }

        if (useRemote) {
            String remote = createViaRemote(sourceStoragePath, bbox, category);
            if (remote != null) {
                return remote;
            }
            log.warn("Remote segmentation unavailable at {}; falling back", remoteUrl);
        }
        if (enabled) {
            String local = createViaLocalShell(sourceStoragePath, bbox, category);
            if (local != null) {
                return local;
            }
        }
        // Guaranteed last resort so every extracted item still gets a white-background image.
        return hasBbox ? createViaJavaFallback(sourceStoragePath, bbox) : null;
    }

    private String createViaRemote(String sourceStoragePath, List<Double> bbox, String category) {
        // One retry: the first request to a sleeping free instance can fail while it wakes.
        for (int attempt = 1; attempt <= 2; attempt++) {
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
                    throw new IllegalStateException("empty response from segmenter");
                }
                try (InputStream in = new ByteArrayInputStream(out)) {
                    String stored = imageStorageService.store(in, "item.png", "items");
                    log.info("Remote cutout created ({} bytes, attempt {}) -> {}", out.length, attempt, stored);
                    return stored;
                }
            } catch (Exception e) {
                log.warn("Remote segmentation attempt {}/2 failed: {}", attempt, e.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
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

    /**
     * Pure-JVM last resort: crop the AI bounding box (with a little padding), upscale small
     * results and centre them on a white square. No native dependencies, so it works on any
     * host and guarantees the wardrobe never shows the raw group photo as a thumbnail.
     */
    private String createViaJavaFallback(String sourceStoragePath, List<Double> bbox) {
        try (InputStream in = imageStorageService.loadAsResource(sourceStoragePath).getInputStream()) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                return null;
            }
            int w = src.getWidth();
            int h = src.getHeight();
            int[] rect = cropRect(bbox, w, h);
            if (rect == null) {
                return null;
            }
            int x = rect[0], y = rect[1], cw = rect[2], ch = rect[3];

            BufferedImage crop = src.getSubimage(x, y, cw, ch);
            BufferedImage rgb = new BufferedImage(crop.getWidth(), crop.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D cg = rgb.createGraphics();
            cg.drawImage(crop, 0, 0, null);
            cg.dispose();

            int side = Math.max(cw, ch);
            int target = Math.max(420, side);
            int pad = (int) Math.round(target * 0.09);
            int canvasSize = target + pad * 2;
            double scale = (double) target / side;
            int drawW = Math.max(1, (int) Math.round(cw * scale));
            int drawH = Math.max(1, (int) Math.round(ch * scale));

            BufferedImage canvas = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, canvasSize, canvasSize);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(rgb, pad + (target - drawW) / 2, pad + (target - drawH) / 2, drawW, drawH, null);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", bos);
            String stored = imageStorageService.store(new ByteArrayInputStream(bos.toByteArray()), "item.png", "items");
            log.info("Java fallback crop created ({}x{}) -> {}", canvasSize, canvasSize, stored);
            return stored;
        } catch (Exception e) {
            log.warn("Java fallback crop failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Accepts the two conventions vision models produce: fractions (0..1) and 0..1000.
     * Returns {@code [x, y, w, h]} in pixels, padded by ~8%, clamped to the image.
     */
    private int[] cropRect(List<Double> bbox, int w, int h) {
        if (bbox == null || bbox.size() != 4) {
            return null;
        }
        double x1 = bbox.get(0), y1 = bbox.get(1), x2 = bbox.get(2), y2 = bbox.get(3);
        double max = Math.max(Math.max(x1, y1), Math.max(x2, y2));
        if (max > 1.5) { // 0..1000 convention
            x1 *= w / 1000.0; x2 *= w / 1000.0;
            y1 *= h / 1000.0; y2 *= h / 1000.0;
        } else {
            x1 *= w; x2 *= w;
            y1 *= h; y2 *= h;
        }
        if (x2 < x1) { double t = x1; x1 = x2; x2 = t; }
        if (y2 < y1) { double t = y1; y1 = y2; y2 = t; }

        double bw = x2 - x1;
        double bh = y2 - y1;
        if (bw < 8 || bh < 8) {
            return null;
        }
        double padX = bw * 0.08;
        double padY = bh * 0.08;
        int px = (int) Math.floor(Math.max(0, x1 - padX));
        int py = (int) Math.floor(Math.max(0, y1 - padY));
        int px2 = (int) Math.ceil(Math.min(w, x2 + padX));
        int py2 = (int) Math.ceil(Math.min(h, y2 + padY));
        int cw = px2 - px;
        int ch = py2 - py;
        if (cw < 8 || ch < 8) {
            return null;
        }
        return new int[]{px, py, cw, ch};
    }
}
