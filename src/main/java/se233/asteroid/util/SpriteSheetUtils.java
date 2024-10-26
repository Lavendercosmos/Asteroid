package se233.asteroid.util;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class SpriteSheetUtils {
    private static final Logger logger = LogManager.getLogger(SpriteSheetUtils.class);

    public static List<Image> extractFrames(String spritePath, int frameCount, boolean isVertical) {
        List<Image> frames = new ArrayList<>();
        try {
            Image spriteSheet = new Image(SpriteSheetUtils.class.getResourceAsStream(spritePath));
            PixelReader reader = spriteSheet.getPixelReader();

            int frameWidth = isVertical ? (int)spriteSheet.getWidth() : (int)(spriteSheet.getWidth() / frameCount);
            int frameHeight = isVertical ? (int)(spriteSheet.getHeight() / frameCount) : (int)spriteSheet.getHeight();

            for (int i = 0; i < frameCount; i++) {
                int x = isVertical ? 0 : i * frameWidth;
                int y = isVertical ? i * frameHeight : 0;

                WritableImage frame = new WritableImage(reader, x, y, frameWidth, frameHeight);
                frames.add(frame);
            }
            logger.debug("Successfully extracted {} frames from sprite sheet: {}", frameCount, spritePath);
        } catch (Exception e) {
            logger.error("Failed to extract frames from sprite sheet: " + spritePath, e);
        }
        return frames;
    }
}
