package se233.asteroid.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpriteSheetUtils {
    private static final Logger logger = LogManager.getLogger(SpriteSheetUtils.class);

    public static List<Image> extractFrames(String resourcePath, int frameCount, boolean isVertical) {
        List<Image> frames = new ArrayList<>();
        try {
            Image spriteSheet = new Image(SpriteSheetUtils.class.getResourceAsStream(resourcePath));
            PixelReader reader = spriteSheet.getPixelReader();

            int frameWidth = isVertical ? (int)spriteSheet.getWidth() : (int)(spriteSheet.getWidth() / frameCount);
            int frameHeight = isVertical ? (int)(spriteSheet.getHeight() / frameCount) : (int)spriteSheet.getHeight();

            for (int i = 0; i < frameCount; i++) {
                int x = isVertical ? 0 : i * frameWidth;
                int y = isVertical ? i * frameHeight : 0;

                WritableImage frame = new WritableImage(reader, x, y, frameWidth, frameHeight);
                frames.add(frame);
            }

            logger.debug("Successfully extracted {} frames from {}", frameCount, resourcePath);
            return frames;
        } catch (Exception e) {
            logger.error("Failed to extract frames from " + resourcePath, e);
            throw new RuntimeException("Failed to load sprite sheet: " + resourcePath, e);
        }
    }

    public static List<Image> extractFramesWithAlpha(String resourcePath, int frameCount, boolean isVertical, int alphaThreshold) {
        List<Image> frames = new ArrayList<>();
        try {
            Image spriteSheet = new Image(SpriteSheetUtils.class.getResourceAsStream(resourcePath));
            PixelReader reader = spriteSheet.getPixelReader();

            int frameWidth = isVertical ? (int)spriteSheet.getWidth() : (int)(spriteSheet.getWidth() / frameCount);
            int frameHeight = isVertical ? (int)(spriteSheet.getHeight() / frameCount) : (int)spriteSheet.getHeight();

            for (int i = 0; i < frameCount; i++) {
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
            }

            logger.debug("Successfully extracted {} frames with alpha from {}", frameCount, resourcePath);
            return frames;
        } catch (Exception e) {
            logger.error("Failed to extract frames with alpha from " + resourcePath, e);
            throw new RuntimeException("Failed to load sprite sheet with alpha: " + resourcePath, e);
        }
    }
}
