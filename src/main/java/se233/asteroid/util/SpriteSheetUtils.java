package se233.asteroid.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpriteSheetUtils {
    private static final Logger logger = LogManager.getLogger(SpriteSheetUtils.class);
    private static final int FALLBACK_SIZE = 32; // Size of fallback image

    public static List<Image> extractFrames(String resourcePath, int frameCount, boolean isVertical) {
        try {
            // Check resource path
            var resourceStream = SpriteSheetUtils.class.getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                logger.error("Sprite sheet not found: {}", resourcePath);
                return createFallbackFrames(frameCount);
            }

            // Load and verify sprite sheet
            Image spriteSheet = new Image(resourceStream);

            // Check if image loaded successfully
            if (spriteSheet.isError()) {
                logger.error("Error loading sprite sheet: {}", resourcePath);
                return createFallbackFrames(frameCount);
            }

            // Validate image dimensions
            if (spriteSheet.getWidth() <= 0 || spriteSheet.getHeight() <= 0) {
                logger.error("Invalid sprite sheet dimensions: {}", resourcePath);
                return createFallbackFrames(frameCount);
            }

            PixelReader reader = spriteSheet.getPixelReader();
            int frameWidth = isVertical ? (int)spriteSheet.getWidth() : (int)(spriteSheet.getWidth() / frameCount);
            int frameHeight = isVertical ? (int)(spriteSheet.getHeight() / frameCount) : (int)spriteSheet.getHeight();

            List<Image> frames = new ArrayList<>();
            for (int i = 0; i < frameCount; i++) {
                try {
                    int x = isVertical ? 0 : i * frameWidth;
                    int y = isVertical ? i * frameHeight : 0;

                    WritableImage frame = new WritableImage(reader, x, y, frameWidth, frameHeight);
                    frames.add(frame);
                } catch (Exception e) {
                    logger.error("Error creating frame {}: {}", i, e.getMessage());
                    frames.add(createFallbackFrame()); // Add fallback frame for failed frame
                }
            }

            if (frames.isEmpty()) {
                logger.warn("No frames could be created, using fallback frames");
                return createFallbackFrames(frameCount);
            }

            logger.debug("Successfully extracted {} frames from {}", frameCount, resourcePath);
            return frames;

        } catch (Exception e) {
            logger.error("Failed to extract frames from {}: {}", resourcePath, e.getMessage());
            return createFallbackFrames(frameCount);
        }
    }

    public static List<Image> extractFramesWithAlpha(String resourcePath, int frameCount, boolean isVertical, int alphaThreshold) {
        try {
            // Check resource path
            var resourceStream = SpriteSheetUtils.class.getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                logger.error("Sprite sheet not found: {}", resourcePath);
                return createFallbackFrames(frameCount);
            }

            Image spriteSheet = new Image(resourceStream);

            // Verify image loading
            if (spriteSheet.isError()) {
                logger.error("Error loading sprite sheet: {}", resourcePath);
                return createFallbackFrames(frameCount);
            }

            // Validate dimensions
            if (spriteSheet.getWidth() <= 0 || spriteSheet.getHeight() <= 0) {
                logger.error("Invalid sprite sheet dimensions: {}", resourcePath);
                return createFallbackFrames(frameCount);
            }

            PixelReader reader = spriteSheet.getPixelReader();
            int frameWidth = isVertical ? (int)spriteSheet.getWidth() : (int)(spriteSheet.getWidth() / frameCount);
            int frameHeight = isVertical ? (int)(spriteSheet.getHeight() / frameCount) : (int)spriteSheet.getHeight();

            List<Image> frames = new ArrayList<>();
            for (int i = 0; i < frameCount; i++) {
                try {
                    int x = isVertical ? 0 : i * frameWidth;
                    int y = isVertical ? i * frameHeight : 0;

                    WritableImage frame = new WritableImage(frameWidth, frameHeight);
                    for (int px = 0; px < frameWidth; px++) {
                        for (int py = 0; py < frameHeight; py++) {
                            int argb = reader.getArgb(x + px, y + py);
                            int alpha = (argb >> 24) & 0xFF;
                            if (alpha >= alphaThreshold) {
                                frame.getPixelWriter().setArgb(px, py, argb);
                            }
                        }
                    }
                    frames.add(frame);
                } catch (Exception e) {
                    logger.error("Error creating alpha frame {}: {}", i, e.getMessage());
                    frames.add(createFallbackFrame());
                }
            }

            if (frames.isEmpty()) {
                logger.warn("No alpha frames could be created, using fallback frames");
                return createFallbackFrames(frameCount);
            }

            logger.debug("Successfully extracted {} alpha frames from {}", frameCount, resourcePath);
            return frames;

        } catch (Exception e) {
            logger.error("Failed to extract alpha frames from {}: {}", resourcePath, e.getMessage());
            return createFallbackFrames(frameCount);
        }
    }

    private static List<Image> createFallbackFrames(int frameCount) {
        List<Image> fallbackFrames = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            fallbackFrames.add(createFallbackFrame());
        }
        return fallbackFrames;
    }

    private static Image createFallbackFrame() {
        WritableImage fallback = new WritableImage(FALLBACK_SIZE, FALLBACK_SIZE);
        var writer = fallback.getPixelWriter();

        // Create simple pattern - red semi-transparent box with white border
        Color fillColor = Color.rgb(255, 0, 0, 0.5); // Red with 50% transparency
        Color borderColor = Color.WHITE;

        // Draw background
        for (int x = 0; x < FALLBACK_SIZE; x++) {
            for (int y = 0; y < FALLBACK_SIZE; y++) {
                // Draw border
                if (x < 2 || x >= FALLBACK_SIZE-2 || y < 2 || y >= FALLBACK_SIZE-2) {
                    writer.setColor(x, y, borderColor);
                } else {
                    writer.setColor(x, y, fillColor);
                }
            }
        }

        return fallback;
    }
}