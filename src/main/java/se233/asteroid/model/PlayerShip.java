package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import se233.asteroid.util.SpriteSheetUtils;

public class PlayerShip extends Character {
    private static final Logger logger = LogManager.getLogger(PlayerShip.class);

    // Movement constants
    private static final double ACCELERATION = 0.5; //ความเร่ง
    private static final double ROTATION_SPEED = 2.0;
    private static final double FRICTION = 0.98; //ความหน่วง
    private static final double MAX_SPEED = 1.0;

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
    private static final String SHIELD_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Shield.png";

    public PlayerShip(Point2D startPosition) {
        super(SHIP_SPRITE_PATH, startPosition, 20);
        initializeShip();
        initializeSprites();  // Move this before setupAnimations
        setupAnimations();
    }

    private void initializeShip() {
        this.lives = 3;
        this.isThrusting = false;
        this.isExploding = false;
        this.isInvulnerable = false;
        this.isAlive = true;

        logger.info("PlayerShip initialized with {} lives", lives);
    }

    private void initializeSprites() {
        try {
            // Setup main ship sprite size
            sprite.setFitWidth(60);
            sprite.setFitHeight(60);
            sprite.setPreserveRatio(true);

            // Initialize thruster sprite with error checking
            var thrusterStream = getClass().getResourceAsStream(THRUSTER_SPRITE_PATH);
            if (thrusterStream == null) {
                throw new RuntimeException("Could not find thruster sprite: " + THRUSTER_SPRITE_PATH);
            }
            thrusterSprite = new ImageView(new Image(thrusterStream));
            thrusterSprite.setFitWidth(30);
            thrusterSprite.setFitHeight(30);
            thrusterSprite.setPreserveRatio(true);
            thrusterSprite.setVisible(false);

            // Initialize shield sprite with error checking
            var shieldStream = getClass().getResourceAsStream(SHIELD_SPRITE_PATH);
            if (shieldStream == null) {
                throw new RuntimeException("Could not find shield sprite: " + SHIELD_SPRITE_PATH);
            }
            shieldSprite = new ImageView(new Image(shieldStream));
            shieldSprite.setFitWidth(60);
            shieldSprite.setFitHeight(60);
            shieldSprite.setPreserveRatio(true);
            shieldSprite.setVisible(false);

            // Load animation frames with error checking
            var explosionStream = getClass().getResourceAsStream(EXPLOSION_SPRITE_PATH);
            if (explosionStream == null) {
                throw new RuntimeException("Could not find explosion sprite: " + EXPLOSION_SPRITE_PATH);
            }
            explosionFrames = SpriteSheetUtils.extractFrames(EXPLOSION_SPRITE_PATH, 8, false);
            if (explosionFrames == null || explosionFrames.isEmpty()) {
                throw new RuntimeException("Failed to extract explosion frames");
            }

            var thrusterAnimStream = getClass().getResourceAsStream(THRUSTER_SPRITE_PATH);
            if (thrusterAnimStream == null) {
                throw new RuntimeException("Could not find thruster animation sprite: " + THRUSTER_SPRITE_PATH);
            }
            thrusterFrames = SpriteSheetUtils.extractFrames(THRUSTER_SPRITE_PATH, 4, false);
            if (thrusterFrames == null || thrusterFrames.isEmpty()) {
                throw new RuntimeException("Failed to extract thruster frames");
            }

            logger.debug("All sprites initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize sprites", e);
            throw new RuntimeException("Failed to initialize ship sprites", e);
        }
    }

    private void setupAnimations() {
        if (explosionFrames == null || explosionFrames.isEmpty()) {
            throw new RuntimeException("Explosion frames not initialized");
        }
        if (thrusterFrames == null || thrusterFrames.isEmpty()) {
            throw new RuntimeException("Thruster frames not initialized");
        }

        // Explosion animation
        explosionAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (currentExplosionFrame < explosionFrames.size()) {
                        sprite.setImage(explosionFrames.get(currentExplosionFrame++));
                    } else {
                        explosionAnimation.stop();
                        respawn();
                    }
                })
        );
        explosionAnimation.setCycleCount(explosionFrames.size());

        // Thruster animation
        thrusterAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (isThrusting) {
                        int frame = (int) (System.currentTimeMillis() / 100 % thrusterFrames.size());
                        thrusterSprite.setImage(thrusterFrames.get(frame));
                    }
                })
        );
        thrusterAnimation.setCycleCount(Timeline.INDEFINITE);

        // Invulnerability animation
        invulnerabilityAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    sprite.setVisible(!sprite.isVisible());
                    shieldSprite.setVisible(sprite.isVisible());
                })
        );
        invulnerabilityAnimation.setCycleCount(30);
    }

    @Override
    public void update() {
        if (!isExploding && isAlive) {
            // Apply friction
            velocity = velocity.multiply(FRICTION);

            // Limit speed
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update position
            super.update();

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
        thrusterSprite.setTranslateX(position.getX() + thrusterOffset.getX() - thrusterSprite.getBoundsInLocal().getWidth() / 2);
        thrusterSprite.setTranslateY(position.getY() + thrusterOffset.getY() - thrusterSprite.getBoundsInLocal().getHeight() / 2);
        thrusterSprite.setRotate(rotation);
    }

    private void updateShieldPosition() {
        shieldSprite.setTranslateX(position.getX() - shieldSprite.getBoundsInLocal().getWidth() / 2);
        shieldSprite.setTranslateY(position.getY() - shieldSprite.getBoundsInLocal().getHeight() / 2);
        shieldSprite.setRotate(rotation);
    }

    public void moveUp() {
        if (!isExploding && isAlive) {
            velocity = velocity.add(new Point2D(0, -ACCELERATION));
            logger.debug("Moving up with velocity: {}", velocity);
        }
    }

    public void moveDown() {
        if (!isExploding && isAlive) {
            velocity = velocity.add(new Point2D(0, ACCELERATION));
            logger.debug("Moving down with velocity: {}", velocity);
        }
    }

    public void moveLeft() {
        if (!isExploding && isAlive) {
            velocity = velocity.add(new Point2D(-ACCELERATION, 0));
            logger.debug("Moving left with velocity: {}", velocity);
        }
    }

    public void moveRight() {
        if (!isExploding && isAlive) {
            velocity = velocity.add(new Point2D(ACCELERATION, 0));
            logger.debug("Moving right with velocity: {}", velocity);
        }
    }

//    public void thrust() {
//        if (!isExploding && isAlive) {
//            isThrusting = true;
//            thrusterSprite.setVisible(true);
//
//            double radians = Math.toRadians(rotation - 90);
//            Point2D thrustVector = new Point2D(
//                    Math.cos(radians) * ACCELERATION,
//                    Math.sin(radians) * ACCELERATION
//            );
//            velocity = velocity.add(thrustVector);
//
//            thrusterAnimation.play();
//            logger.debug("Thrusting in direction: {} degrees", rotation);
//        }
//    }

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

    public se233.asteroid.model.Bullet shoot() {
        if (!isExploding && isAlive) {
            double radians = Math.toRadians(rotation - 90);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Point2D bulletPosition = position.add(direction.multiply(sprite.getBoundsInLocal().getWidth() / 2));
            logger.info("Shooting bullet from position: {}", bulletPosition);
            // Use the fully qualified class name to avoid confusion
            return new se233.asteroid.model.Bullet(bulletPosition, direction, false);
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

    private void explode() {
        isExploding = true;
        currentExplosionFrame = 0;

        // หยุดการเคลื่อนที่
        velocity = new Point2D(0, 0);

        // ปรับขนาด sprite สำหรับการระเบิด
        sprite.setFitWidth(hitRadius * 4);
        sprite.setFitHeight(hitRadius * 4);

        // ปรับตำแหน่งให้ centered
        sprite.setTranslateX(position.getX() - sprite.getFitWidth()/2);
        sprite.setTranslateY(position.getY() - sprite.getFitHeight()/2);

        explosionAnimation.play();
        explosionAnimation.setOnFinished(e -> {
            sprite.setVisible(false);  // ซ่อน sprite
            sprite.setImage(null);     // ลบรูปภาพ
            isAlive = false;           // ตั้งค่าว่าตายแล้ว
        });

        stopThrust(); // หยุด thruster
        logger.info("Ship explosion started");
    }

    private void respawn() {
        if (lives > 0) {
            isExploding = false;
            position = new Point2D(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
            velocity = new Point2D(0, 0);
            rotation = 0;
            startInvulnerability();
            logger.info("Ship respawned at center position");
        } else {
            isAlive = false;
            sprite.setVisible(false);
            sprite.setImage(null);
            // หยุดทุก animation
            stopAllAnimations();
            logger.info("Game over - no lives remaining");
        }
    }

    private void stopAllAnimations() {
        if (explosionAnimation != null) explosionAnimation.stop();
        if (thrusterAnimation != null) thrusterAnimation.stop();
        if (invulnerabilityAnimation != null) invulnerabilityAnimation.stop();
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

    public void activateShield() {
        if (isAlive && !isExploding) {
            isInvulnerable = true;
            shieldSprite.setVisible(true);

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

    public void reset() {
        lives = 3;
        position = new Point2D(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
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

    public void dispose() {
        if (explosionAnimation != null) explosionAnimation.stop();
        if (thrusterAnimation != null) thrusterAnimation.stop();
        if (invulnerabilityAnimation != null) invulnerabilityAnimation.stop();
        logger.info("PlayerShip resources disposed");
    }

    public boolean isExploding() {
        return isExploding;
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public int getLives() {
        return lives;
    }

    public boolean isAlive() {
        return isAlive;
    }


    public ImageView getThrusterSprite() {
        return thrusterSprite;
    }

    public ImageView getShieldSprite() {
        return shieldSprite;
    }

    // Additional helper class - Bullet
    public static class Bullet extends Character {
        private static final double BULLET_SPEED = 10.0;
        private static final String BULLET_SPRITE_PATH = "src/main/resources/se233/asteroid/assets/PlayerShip/Fx_01.png";
        private static final double BULLET_RADIUS = 2.0;
        private static final double BULLET_LIFETIME = 2.0; // seconds
        private double lifetime;

        public Bullet(Point2D position, Point2D direction) {
            super(BULLET_SPRITE_PATH, position, BULLET_RADIUS);
            this.velocity = direction.multiply(BULLET_SPEED);
            this.lifetime = BULLET_LIFETIME;

            // Set bullet size
            sprite.setFitWidth(4);
            sprite.setFitHeight(4);
        }

        @Override
        public void update() {
            super.update();
            lifetime -= 1.0 / 60.0; // Assuming 60 FPS
        }

        public boolean isExpired() {
            return lifetime <= 0;
        }
    }
}



