package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;


public class Asteroid extends Character {
    private static final Logger logger = LogManager.getLogger(Asteroid.class);

    // Asset paths
    private static final String ASTEROID_BASE_PATH = "/se233/asteroid/assets/Astroides/Asteroid_Base.png";
    private static final String ASTEROID_EXPLODE_PATH = "/se233/asteroid/assets/Astroides/Asteroid_Explode.png";

    // Constants
    private static final double SMALL_SPEED = 3.0;
    private static final double LARGE_SPEED = 2.0;
    private static final double SMALL_ROTATION_SPEED = 2.0;
    private static final double LARGE_ROTATION_SPEED = 1.0;
    private static final double SMALL_RADIUS = 15;
    private static final double LARGE_RADIUS = 30;
    private static final int EXPLOSION_FRAMES = 8;
    private static final Duration FRAME_DURATION = Duration.millis(100);

    // Asteroid properties
    private final int size; // 1 for small, 2 for large
    private final int points;
    private final double rotationSpeed;
    private boolean isExploding;
    private List<Image> explosionFrames;
    private int currentExplosionFrame;
    private Timeline explosionAnimation;

    public Asteroid(Point2D position, int size) {
        super(ASTEROID_BASE_PATH, position, size == 1 ? SMALL_RADIUS : LARGE_RADIUS);

        this.size = size;
        this.points = size;
        this.rotationSpeed = size == 1 ? SMALL_ROTATION_SPEED : LARGE_ROTATION_SPEED;
        this.isExploding = false;

        // Initialize explosion frames
        this.explosionFrames = loadExplosionFrames();
        setupExplosionAnimation();

        // Set random velocity based on size
        initializeVelocity();

        // Set random initial rotation
        this.rotation = Math.random() * 360;

        logger.info("Created {} asteroid at position: {}",
                size == 1 ? "small" : "large", position);
    }

    private List<Image> loadExplosionFrames() {
        List<Image> frames = new ArrayList<>();
        try {
            Image explodeSheet = new Image(getClass().getResourceAsStream(ASTEROID_EXPLODE_PATH));

            double frameWidth = explodeSheet.getWidth() / EXPLOSION_FRAMES;
            double frameHeight = explodeSheet.getHeight();

            for (int i = 0; i < EXPLOSION_FRAMES; i++) {
                Canvas canvas = new Canvas(frameWidth, frameHeight);
                GraphicsContext gc = canvas.getGraphicsContext2D();

                // Draw the specific frame from the sprite sheet
                gc.drawImage(explodeSheet,
                        i * frameWidth, 0,          // source position
                        frameWidth, frameHeight,    // source dimensions
                        0, 0,                       // destination position
                        frameWidth, frameHeight);   // destination dimensions

                // Create transparent snapshot
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                Image frame = canvas.snapshot(params, null);
                frames.add(frame);
            }

            logger.debug("Loaded {} explosion frames", frames.size());

        } catch (Exception e) {
            logger.error("Failed to load explosion frames", e);
        }
        return frames;
    }

    private void setupExplosionAnimation() {
        explosionAnimation = new Timeline(
                new KeyFrame(FRAME_DURATION, e -> {
                    if (currentExplosionFrame < explosionFrames.size()) {
                        sprite.setImage(explosionFrames.get(currentExplosionFrame));
                        currentExplosionFrame++;
                    } else {
                        explosionAnimation.stop();
                        isAlive = false;
                        logger.debug("Explosion animation completed");
                    }
                })
        );
        explosionAnimation.setCycleCount(explosionFrames.size());
    }

    private void initializeVelocity() {
        double speed = size == 1 ? SMALL_SPEED : LARGE_SPEED;
        double angle = Math.random() * 2 * Math.PI;
        this.velocity = new Point2D(
                Math.cos(angle) * speed,
                Math.sin(angle) * speed
        );
    }



    @Override
    public void update() {
        if (!isExploding) {
            // Update position
            super.update();

            // Update rotation
            rotation += rotationSpeed;

            // Keep rotation between 0 and 360 degrees
            if (rotation >= 360) {
                rotation -= 360;
            }
        }
    }

    public void explode() {
        if (!isExploding) {
            isExploding = true;
            currentExplosionFrame = 0;

            // Adjust sprite size for explosion
            sprite.setFitWidth(size == 1 ? SMALL_RADIUS * 4 : LARGE_RADIUS * 4);
            sprite.setFitHeight(size == 1 ? SMALL_RADIUS * 4 : LARGE_RADIUS * 4);

            // Center the explosion
            sprite.setTranslateX(position.getX() - sprite.getFitWidth()/2);
            sprite.setTranslateY(position.getY() - sprite.getFitHeight()/2);

            // Start explosion animation
            explosionAnimation.play();
            logger.info("Started explosion animation for {} asteroid",
                    size == 1 ? "small" : "large");
        }
    }

    public List<Asteroid> split() {
        List<Asteroid> fragments = new ArrayList<>();
        if (size == 2) { // Only large asteroids split
            for (int i = 0; i < 2; i++) {
                // Create slightly offset positions for fragments
                double offsetX = (Math.random() - 0.5) * 20;
                double offsetY = (Math.random() - 0.5) * 20;
                Point2D fragmentPos = new Point2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );
                fragments.add(new Asteroid(fragmentPos, 1));
            }
            logger.info("Large asteroid split into {} smaller fragments", fragments.size());
        }
        return fragments;
    }

    @Override
    protected void updateSpritePosition() {
        if (!isExploding) {
            super.updateSpritePosition();
        }
    }

    @Override
    public void setPosition(Point2D newPosition) {
        this.position = newPosition;
        if (!isExploding) {
            updateSpritePosition();
        }
    }

    // Getters
    public int getSize() { return size; }
    public int getPoints() { return points; }
    public boolean isExploding() { return isExploding; }
    public double getRadius() {
        return this.size == 1 ? SMALL_RADIUS : LARGE_RADIUS;
    }
    // Resource cleanup
    public void dispose() {
        if (explosionAnimation != null) {
            explosionAnimation.stop();
        }
    }
}