package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Bullet extends Character {
    private static final Logger logger = LogManager.getLogger(Bullet.class);

    // Constants
    private static final double BULLET_SPEED = 10.0;
    private static final String BULLET_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Fx_01.png";
    private static final double BULLET_SIZE = 15.0;
    private static final double BULLET_LIFETIME = 2.0; // seconds
    private static final int BULLET_DAMAGE = 10;

    // State
    private boolean active;
    private double lifetime;
    private boolean isEnemyBullet;


    public Bullet(Point2D position, Point2D direction) {
        this(position, direction, false);
    }


    public Bullet(Point2D position, Point2D direction, boolean isEnemyBullet) {
        super(BULLET_SPRITE_PATH, position, BULLET_SIZE);
        initializeBullet(direction, isEnemyBullet);
    }

    private void initializeBullet(Point2D direction, boolean isEnemyBullet) {
        try {
            // Initialize state
            this.active = true;
            this.lifetime = BULLET_LIFETIME;
            this.isEnemyBullet = isEnemyBullet;
            this.velocity = direction.normalize().multiply(BULLET_SPEED);

            // Configure sprite
            configureSprite();

            logger.debug("Bullet initialized at position: {} with velocity: {}, isEnemyBullet: {}",
                    position, velocity, isEnemyBullet);
        } catch (Exception e) {
            logger.error("Failed to initialize bullet", e);
            throw new RuntimeException("Failed to initialize bullet", e);
        }
    }

    private void configureSprite() {
        // Set size
        sprite.setFitWidth(BULLET_SIZE * 2);
        sprite.setFitHeight(BULLET_SIZE * 2);
        sprite.setPreserveRatio(true);

        // If it's an enemy bullet, tint it red
        if (isEnemyBullet) {
            sprite.setStyle("-fx-effect: dropshadow(gaussian, red, 10, 0.5, 0, 0);");
        }

        // Set rotation based on velocity direction
        double angle = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));
        sprite.setRotate(angle + 90); // +90 because sprite points upward by default
    }

    @Override
    public void update() {
        if (active) {
            // Update position
            super.update();

            // Update lifetime
            lifetime -= 1.0 / 60.0; // Assuming 60 FPS
            if (lifetime <= 0) {
                deactivate();
            }

            // Update sprite rotation if needed
            updateSpriteRotation();
        }
    }

    @Override
    protected void updateSpriteRotation() {
        if (velocity.magnitude() > 0) {
            double angle = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));
            sprite.setRotate(angle + 90);
        }
    }

    /**
     * Deactivates the bullet, making it ready for removal.
     */
    public void deactivate() {
        active = false;
        sprite.setVisible(false);
        logger.debug("Bullet deactivated at position: {}", position);
    }

    public boolean canDamage(Character character) {
        if (character instanceof PlayerShip) {
            return isEnemyBullet;
        } else {
            return !isEnemyBullet;
        }
    }

    // Getters and setters
    @Override
    public boolean isAlive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        sprite.setVisible(active);
    }

    public boolean isExpired() {
        return lifetime <= 0;
    }

    public boolean isEnemyBullet() {
        return isEnemyBullet;
    }

    public int getDamage() {
        return BULLET_DAMAGE;
    }

    @Override
    public ImageView getSprite() {
        return sprite;
    }

    public double getRadius() {
        return BULLET_SIZE;
    }

    @Override
    public String toString() {
        return String.format("Bullet[position=(%f,%f), velocity=(%f,%f), active=%b, isEnemy=%b]",
                position.getX(), position.getY(),
                velocity.getX(), velocity.getY(),
                active, isEnemyBullet);
    }

    /**
     * Disposes of any resources used by this bullet.
     */
    public void dispose() {
        sprite.setImage(null);
        active = false;
        logger.debug("Bullet disposed");
    }
}