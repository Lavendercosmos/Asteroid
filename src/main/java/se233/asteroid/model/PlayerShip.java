package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import se233.asteroid.util.SpriteSheetUtils;

public class PlayerShip extends Character {
    private static final Logger logger = LogManager.getLogger(PlayerShip.class);

    // Movement constants
    private static final double MOVEMENT_SPEED = 5.0;
    private static final double ROTATION_SPEED = 5.0;
    private static final double FRICTION = 0.98;
    private static final double MAX_SPEED = 10.0;

    // Screen boundaries
    private static final double SCREEN_WIDTH = 800.0;
    private static final double SCREEN_HEIGHT = 600.0;

    // Ship states
    private int lives;
    private boolean isThrusting;
    private boolean isExploding;
    private boolean isInvulnerable;
    private boolean isAlive;

    // Sprite management
    private ImageView thrusterSprite;
    private ImageView shieldSprite;
    private List<Image> explosionFrames;
    private List<Image> thrusterFrames;
    private int currentExplosionFrame;

    // Animations
    private Timeline explosionAnimation;
    private Timeline thrusterAnimation;
    private Timeline invulnerabilityAnimation;

    // Asset paths
    private static final String SHIP_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Spaceships.png";
    private static final String THRUSTER_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Thruster_01.png";
    private static final String EXPLOSION_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Explosion.png";
    private static final String SHIELD_EFFECT_PATH = "/se233/asteroid/assets/PlayerShip/Fx_01.png";

    public PlayerShip(Point2D startPosition) {
        super(SHIP_SPRITE_PATH, startPosition, 20);  // 20 is the collision radius
        this.lives = 3;
        this.isThrusting = false;
        this.isExploding = false;
        this.isInvulnerable = false;
        this.isAlive = true;
        this.position = startPosition;
        this.velocity = new Point2D(0, 0);
        this.rotation = 0;

        initializeSprites();
        setupAnimations();

        logger.info("PlayerShip created with {} lives at position: {}", lives, startPosition);
    }

    private void initializeSprites() {
        try {
            // Initialize main ship sprite
            sprite.setFitWidth(40);  // Set appropriate size for ship
            sprite.setFitHeight(40);
            sprite.setPreserveRatio(true);

            // Initialize thruster sprite
            Image thrusterImage = new Image(getClass().getResourceAsStream(THRUSTER_SPRITE_PATH));
            thrusterSprite = new ImageView(thrusterImage);
            thrusterSprite.setFitWidth(30);  // Slightly smaller than ship
            thrusterSprite.setFitHeight(30);
            thrusterSprite.setPreserveRatio(true);
            thrusterSprite.setVisible(false);

            // Load animation frames
            explosionFrames = loadExplosionFrames();
            thrusterFrames = loadThrusterFrames();

            // Initialize shield effect
            Image shieldImage = new Image(getClass().getResourceAsStream(SHIELD_EFFECT_PATH));
            shieldSprite = new ImageView(shieldImage);
            shieldSprite.setFitWidth(60);  // Larger than ship to encompass it
            shieldSprite.setFitHeight(60);
            shieldSprite.setPreserveRatio(true);
            shieldSprite.setVisible(false);

            logger.debug("All sprites initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize sprites", e);
        }
    }

    private List<Image> loadExplosionFrames() {
        return SpriteSheetUtils.extractFrames(EXPLOSION_SPRITE_PATH, 8, false);
    }

    private List<Image> loadThrusterFrames() {
        return SpriteSheetUtils.extractFrames(THRUSTER_SPRITE_PATH, 4, false);
    }

    private void setupAnimations() {
        // Setup explosion animation
        explosionAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (currentExplosionFrame < explosionFrames.size()) {
                        sprite.setImage(explosionFrames.get(currentExplosionFrame));
                        currentExplosionFrame++;
                    } else {
                        explosionAnimation.stop();
                        respawn();
                    }
                })
        );
        explosionAnimation.setCycleCount(explosionFrames.size());

        // Setup thruster animation
        thrusterAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (isThrusting) {
                        int frame = (int) (System.currentTimeMillis() / 100 % thrusterFrames.size());
                        thrusterSprite.setImage(thrusterFrames.get(frame));
                    }
                })
        );
        thrusterAnimation.setCycleCount(Timeline.INDEFINITE);

        // Setup invulnerability blinking
        invulnerabilityAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    sprite.setVisible(!sprite.isVisible());
                    shieldSprite.setVisible(sprite.isVisible());
                })
        );
        invulnerabilityAnimation.setCycleCount(30); // 3 seconds of blinking
    }

    @Override
    public void update() {
        if (!isExploding && isAlive) {
            // Apply friction
            velocity = velocity.multiply(FRICTION);

            // Limit maximum speed
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update position
            position = position.add(velocity);

            // Handle screen wrapping
            position = new Point2D(
                    (position.getX() + SCREEN_WIDTH) % SCREEN_WIDTH,
                    (position.getY() + SCREEN_HEIGHT) % SCREEN_HEIGHT
            );

            // Update sprite positions
            sprite.setTranslateX(position.getX() - sprite.getBoundsInLocal().getWidth()/2);
            sprite.setTranslateY(position.getY() - sprite.getBoundsInLocal().getHeight()/2);
            sprite.setRotate(rotation);

            // Update auxiliary sprites
            if (isThrusting) {
                updateThrusterPosition();
            }
            if (isInvulnerable) {
                updateShieldPosition();
            }
        }
    }

    private void updateThrusterPosition() {
        double radians = Math.toRadians(rotation - 90);
        Point2D thrusterOffset = new Point2D(Math.cos(radians) * -20, Math.sin(radians) * -20);
        thrusterSprite.setTranslateX(position.getX() + thrusterOffset.getX() - thrusterSprite.getBoundsInLocal().getWidth()/2);
        thrusterSprite.setTranslateY(position.getY() + thrusterOffset.getY() - thrusterSprite.getBoundsInLocal().getHeight()/2);
        thrusterSprite.setRotate(rotation);
    }

    private void updateShieldPosition() {
        shieldSprite.setTranslateX(position.getX() - shieldSprite.getBoundsInLocal().getWidth()/2);
        shieldSprite.setTranslateY(position.getY() - shieldSprite.getBoundsInLocal().getHeight()/2);
        shieldSprite.setRotate(rotation);
    }

    public void thrust() {
        if (!isExploding && isAlive) {
            isThrusting = true;
            thrusterSprite.setVisible(true);
            double radians = Math.toRadians(rotation - 90);
            Point2D thrustVector = new Point2D(Math.cos(radians), Math.sin(radians)).multiply(MOVEMENT_SPEED);
            velocity = velocity.add(thrustVector);
            thrusterAnimation.play();
            logger.debug("Thrusting in direction: {} degrees", rotation);
        }
    }

    public void moveLeft() {
        if (!isExploding && isAlive) {
            // Add velocity in the left direction
            velocity = velocity.add(new Point2D(-MOVEMENT_SPEED, 0));

            // Apply speed limit
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update rotation to face movement direction
            rotation = 270;
            logger.debug("Moving left with velocity: {}", velocity);
        }
    }

    public void moveRight() {
        if (!isExploding && isAlive) {
            // Add velocity in the right direction
            velocity = velocity.add(new Point2D(MOVEMENT_SPEED, 0));

            // Apply speed limit
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update rotation to face movement direction
            rotation = 90;
            logger.debug("Moving right with velocity: {}", velocity);
        }
    }

    public void moveUp() {
        if (!isExploding && isAlive) {
            // Add velocity in the upward direction
            velocity = velocity.add(new Point2D(0, -MOVEMENT_SPEED));

            // Apply speed limit
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update rotation to face movement direction
            rotation = 0;
            logger.debug("Moving up with velocity: {}", velocity);
        }
    }

    public void moveDown() {
        if (!isExploding && isAlive) {
            // Add velocity in the downward direction
            velocity = velocity.add(new Point2D(0, MOVEMENT_SPEED));

            // Apply speed limit
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update rotation to face movement direction
            rotation = 180;
            logger.debug("Moving down with velocity: {}", velocity);
        }
    }

    public void stopThrust() {
        isThrusting = false;
        thrusterSprite.setVisible(false);
        thrusterAnimation.stop();
    }

    public void rotateLeft() {
        if (!isExploding && isAlive) {
            rotation = (rotation - ROTATION_SPEED + 360) % 360;
            logger.debug("Rotating left to {} degrees", rotation);
        }
    }

    public void rotateRight() {
        if (!isExploding && isAlive) {
            rotation = (rotation + ROTATION_SPEED) % 360;
            logger.debug("Rotating right to {} degrees", rotation);
        }
    }

    public Bullet shoot() {
        if (!isExploding && isAlive) {
            double radians = Math.toRadians(rotation - 90);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Point2D bulletPosition = position.add(direction.multiply(sprite.getBoundsInLocal().getWidth() / 2));
            logger.info("Shooting bullet from position: {}", bulletPosition);
            return new Bullet(bulletPosition, direction);
        }
        return null;
    }

    public void hit() {
        if (!isInvulnerable && !isExploding && isAlive) {
            lives--;
            logger.info("Ship hit! Lives remaining: {}", lives);

            if (lives <= 0) {
                explode();
            } else {
                startInvulnerability();
            }
        }
    }

    public void activateShield() {
        if (isAlive && !isExploding) {
            isInvulnerable = true;
            shieldSprite.setVisible(true);

            // Create a timer to deactivate the shield after a few seconds
            Timeline shieldTimer = new Timeline(
                    new KeyFrame(Duration.seconds(5), e -> {
                        isInvulnerable = false;
                        shieldSprite.setVisible(false);
                    })
            );
            shieldTimer.play();

            logger.debug("Shield activated");
        }
    }

    private void explode() {
        isExploding = true;
        currentExplosionFrame = 0;
        explosionAnimation.play();
        stopThrust();
        logger.info("Ship explosion started");
    }

    private void respawn() {
        if (lives > 0) {
            isExploding = false;
            position = new Point2D(SCREEN_WIDTH/2, SCREEN_HEIGHT/2);
            velocity = new Point2D(0, 0);
            rotation = 0;
            startInvulnerability();
            logger.info("Ship respawned at center position");
        } else {
            isAlive = false;
            sprite.setVisible(false);
            logger.info("Game over - no lives remaining");
        }
    }


    private void startInvulnerability() {
        isInvulnerable = true;
        invulnerabilityAnimation.play();
        Timeline invulnerabilityTimer = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    isInvulnerable = false;
                    sprite.setVisible(true);
                    shieldSprite.setVisible(false);
                })
        );
        invulnerabilityTimer.play();
        logger.debug("Invulnerability started");
    }
    public void addLife() {
        if (isAlive) {
            lives++;
            logger.info("Extra life added. Total lives: {}", lives);
        }
    }

    public void reset() {
        lives = 3;
        position = new Point2D(SCREEN_WIDTH/2, SCREEN_HEIGHT/2);
        velocity = new Point2D(0, 0);
        rotation = 0;
        isExploding = false;
        isInvulnerable = false;
        isThrusting = false;
        isAlive = true;
        sprite.setVisible(true);
        shieldSprite.setVisible(false);
        thrusterSprite.setVisible(false);
        logger.info("Ship reset to initial state");
    }

    // Getters
    public int getLives() { return lives; }
    public boolean isExploding() { return isExploding; }
    public boolean isInvulnerable() { return isInvulnerable; }
    public boolean isAlive() { return isAlive; }
    public ImageView getThrusterSprite() { return thrusterSprite; }
     public ImageView getShieldSprite() { return shieldSprite; }
    public double getRadius() {
        return 20.0; // หรือค่าอื่นที่เหมาะสม
    }

    // Clean up resources
    public void dispose() {
        if (explosionAnimation != null) explosionAnimation.stop();
        if (thrusterAnimation != null) thrusterAnimation.stop();
        if (invulnerabilityAnimation != null) invulnerabilityAnimation.stop();
        logger.info("PlayerShip resources disposed");
    }
}