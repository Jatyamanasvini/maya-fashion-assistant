package com.maya.fashionassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the outfit visualization as a "look board" composed from the user's ACTUAL
 * wardrobe item images — never an AI-invented picture. Renders on a plain white
 * background using Java2D; no external service required.
 */
@Service
public class OutfitVisualizationService {

    private static final Logger log = LoggerFactory.getLogger(OutfitVisualizationService.class);

    private static final int CELL_W = 420;
    private static final int CELL_H = 460;
    private static final int GAP = 20;
    private static final int MARGIN = 28;
    private static final int HEADER = 76;

    private final ImageStorageService imageStorageService;

    public OutfitVisualizationService(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    /** A single item to place on the board. */
    public record Piece(String storagePath, String role) {
    }

    /**
     * Composes a white look board from the given items.
     *
     * @return PNG bytes, or {@code null} when no item images are available
     */
    public byte[] composeBoard(List<Piece> pieces) {
        List<BufferedImage> images = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Piece piece : pieces) {
            if (piece.storagePath() == null) {
                continue;
            }
            try (InputStream in = imageStorageService.loadAsResource(piece.storagePath()).getInputStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    images.add(image);
                    labels.add(piece.role());
                }
            } catch (Exception e) {
                log.warn("Could not load item image {}: {}", piece.storagePath(), e.getMessage());
            }
        }
        if (images.isEmpty()) {
            return null;
        }

        int n = images.size();
        int cols = Math.min(3, Math.max(1, (int) Math.ceil(Math.sqrt(n))));
        int rows = (int) Math.ceil((double) n / cols);

        int width = MARGIN * 2 + cols * CELL_W + (cols - 1) * GAP;
        int height = HEADER + MARGIN + rows * CELL_H + (rows - 1) * GAP + MARGIN;

        BufferedImage board = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = board.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(0x33, 0x33, 0x33));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("MAYA", MARGIN, 50);
        g.setColor(new Color(0x99, 0x99, 0x99));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("your curated look", MARGIN + 96, 50);

        for (int i = 0; i < n; i++) {
            int row = i / cols;
            int col = i % cols;
            int x = MARGIN + col * (CELL_W + GAP);
            int y = HEADER + MARGIN + row * (CELL_H + GAP);

            g.setColor(new Color(0xE6, 0xE6, 0xE6));
            g.drawRoundRect(x, y, CELL_W, CELL_H, 18, 18);

            BufferedImage image = images.get(i);
            int maxW = CELL_W - 48;
            int maxH = CELL_H - 96;
            double scale = Math.min((double) maxW / image.getWidth(), (double) maxH / image.getHeight());
            int iw = Math.max(1, (int) (image.getWidth() * scale));
            int ih = Math.max(1, (int) (image.getHeight() * scale));
            int ix = x + (CELL_W - iw) / 2;
            int iy = y + 24 + (maxH - ih) / 2;
            g.drawImage(image, ix, iy, iw, ih, null);

            String label = labels.get(i) == null ? "" : labels.get(i);
            g.setColor(new Color(0x33, 0x33, 0x33));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, x + (CELL_W - fm.stringWidth(label)) / 2, y + CELL_H - 20);
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(board, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Failed to encode outfit board: {}", e.getMessage());
            return null;
        }
    }
}
