package se233.asteroid.util;

import javafx.scene.image.ImageView;
import java.util.List;

/**
 * Interface for game entities that have visual effects.
 * Entities implementing this interface can have additional sprite effects
 * like thrusters, shields, or other visual indicators.
 */
public interface EntityWithEffects {
    /**
     * Gets a list of all effect sprites associated with this entity.
     * These sprites are rendered in addition to the main entity sprite.
     *
     * @return List of ImageView objects representing the effects
     */
    List<ImageView> getEffectSprites();

    /**
     * Updates the position and state of all effect sprites.
     * This should be called whenever the entity moves or changes state.
     */
    void updateEffectSprites();

    /**
     * Shows or hides specific effect sprites based on the entity's state.
     *
     * @param effectType The type of effect to toggle
     * @param visible Whether the effect should be visible
     */
    void setEffectVisible(String effectType, boolean visible);

    /**
     * Cleans up and removes all effect sprites.
     * Should be called when the entity is destroyed or removed from the game.
     */
    void disposeEffects();
}
