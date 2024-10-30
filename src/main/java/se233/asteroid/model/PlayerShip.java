package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import se233.asteroid.util.SpriteSheetUtils;

import static se233.asteroid.model.Character.FRICTION;
import static se233.asteroid.model.Character.MAX_SPEED;

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
    public boolean isExploding;
    private boolean isShootingEffect;
    private boolean isInvulnerable;
    private boolean isAlive;


    // Sprite management
    private ImageView thrusterSprite;
    private ImageView ShootingEffect;
    private List<Image> explosionFrames;
    private List<Image> thrusterFrames;
    private List<Image> ShootingFrames;
    private int currentExplosionFrame;
    private int currentThrusterFrame;
    private int currentShootingFrame;

    // Animations
    private Timeline explosionAnimation;
    private Timeline thrusterAnimation;
    private Timeline invulnerabilityAnimation;
    private Timeline ShootingAnimation;

    // Asset paths
    private static final String SHIP_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Spaceships.png";
    private static final String THRUSTER_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Thruster_01.png";
    private static final String EXPLOSION_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/Explosion.png";
    private static final String SHOOTING_EFFECT_PATH = "/se233/asteroid/assets/PlayerShip/ShootEffect.png";

    public PlayerShip(Point2D startPosition) {
        super(SHIP_SPRITE_PATH, startPosition, 5);
        initializeShip();
        initializeSprites();  // Move this before setupAnimations
        setupAnimations();
    }

    public void startThrust() {
        if (!isThrusting && !isExploding && isAlive) {
            isThrusting = true;
            currentThrusterFrame = 0;

            // Show thruster sprite
            thrusterSprite.setVisible(true);

            // Stop any existing animation
            if (thrusterAnimation != null) {
                thrusterAnimation.stop();
            }

            // Create new thruster animation
            thrusterAnimation = new Timeline(
                    new KeyFrame(Duration.millis(50), e -> {
                        // Calculate the next frame index (0-3 cycling)
                        int frameIndex = (currentThrusterFrame++) % thrusterFrames.size();

                        // Update thruster sprite with current frame
                        thrusterSprite.setImage(thrusterFrames.get(frameIndex));

                        // Update thruster position relative to ship
                        updateThrusterPosition();
                    })
            );

            // Set animation to repeat indefinitely
            thrusterAnimation.setCycleCount(Timeline.INDEFINITE);
            thrusterAnimation.play();

            logger.debug("Thruster animation started");
        }
    }

    public void startShootingEffect(){
        if (!isShootingEffect && !isExploding && isAlive) {
            isShootingEffect = true;
            currentShootingFrame = 0;

            ShootingEffect.setVisible(true);
            updateShootEffectPosition();

            if(ShootingAnimation !=null){
                ShootingAnimation.stop();
            }

            ShootingAnimation = new Timeline(
                    new KeyFrame(Duration.millis(50), e -> {
                      int frameIndex = (currentShootingFrame++) % ShootingFrames.size();

                      ShootingEffect.setImage(ShootingFrames.get(frameIndex));

                      updateShootEffectPosition();
                    })
            );
            // Set to play once
            ShootingAnimation.setCycleCount(ShootingFrames.size());
            ShootingAnimation.setOnFinished(e -> {
                ShootingEffect.setVisible(false);
                isShootingEffect = false;
            });

            ShootingAnimation.play();

        }
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
            sprite.setFitWidth(30);
            sprite.setFitHeight(30);
            sprite.setPreserveRatio(true);

            // Initialize thruster sprite with error checking
            var thrusterStream = getClass().getResourceAsStream(THRUSTER_SPRITE_PATH);
            if (thrusterStream == null) {
                throw new RuntimeException("Could not find thruster sprite: " + THRUSTER_SPRITE_PATH);
            }

            thrusterSprite = new ImageView();
            thrusterSprite.setFitWidth(20);
            thrusterSprite.setFitHeight(20);
            thrusterSprite.setPreserveRatio(true);
            thrusterSprite.setVisible(false);

            ShootingEffect = new ImageView();
            ShootingEffect.setFitWidth(20);
            ShootingEffect.setFitHeight(20);
            ShootingEffect.setPreserveRatio(true);
            ShootingEffect.setVisible(false);

            // Load all sprite sheets
            loadExplosionFrames();
            loadThrusterFrames();
            loadShootingFrames();
            logger.debug("All sprites initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize sprites", e);
            throw new RuntimeException("Failed to initialize sprites", e);
        }
    }

        private void loadExplosionFrames() {
            var explosionStream = getClass().getResourceAsStream(EXPLOSION_SPRITE_PATH);
            if (explosionStream == null) {
                throw new RuntimeException("Could not find explosion sprite: " + EXPLOSION_SPRITE_PATH);
            }
            explosionFrames = SpriteSheetUtils.extractFrames(EXPLOSION_SPRITE_PATH, 8, false);
            if (explosionFrames == null || explosionFrames.isEmpty()) {
                throw new RuntimeException("Failed to extract explosion frames");
            }
        }

        private void loadThrusterFrames() {
            var thrusterStream = getClass().getResourceAsStream(THRUSTER_SPRITE_PATH);
            if (thrusterStream == null) {
                throw new RuntimeException("Could not find thruster sprite: " + THRUSTER_SPRITE_PATH);
            }
            thrusterFrames = SpriteSheetUtils.extractFrames(THRUSTER_SPRITE_PATH, 4, false);
            if (thrusterFrames == null || thrusterFrames.isEmpty()) {
                throw new RuntimeException("Failed to extract thruster frames");
            }
        }

        private void loadShootingFrames() {
            var shootingStream = getClass().getResourceAsStream(SHOOTING_EFFECT_PATH);
            if (shootingStream == null) {
                throw new RuntimeException("Could not find shooting effect sprite: " + SHOOTING_EFFECT_PATH);
            }
            ShootingFrames = SpriteSheetUtils.extractFrames(SHOOTING_EFFECT_PATH, 4, false);
            if (ShootingFrames == null || ShootingFrames.isEmpty()) {
                throw new RuntimeException("Failed to extract shooting frames");
            }
        }

    private Image createEmptyImage(int width, int height) {
        return new WritableImage(width, height);
    }

    private void setupAnimations() {
        if (explosionFrames == null || explosionFrames.isEmpty()) {
            throw new RuntimeException("Explosion frames not initialized");
        }
        if (thrusterFrames == null || thrusterFrames.isEmpty()) {
            throw new RuntimeException("Thruster frames not initialized");
        }
        if (ShootingFrames == null || ShootingFrames.isEmpty()){
            throw new RuntimeException("Shooting frames not initialized");
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
                    if (currentThrusterFrame < thrusterFrames.size()) {
                        int frame = (int) (System.currentTimeMillis() / 100 % thrusterFrames.size());
                        sprite.setImage(thrusterFrames.get(currentThrusterFrame++));
                    }
                })
        );
        thrusterAnimation.setCycleCount(thrusterFrames.size());

        ShootingAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    if (currentShootingFrame < ShootingFrames.size()){
                        int frame = (int) (System.currentTimeMillis()/100 % ShootingFrames.size());
                        sprite.setImage(ShootingFrames.get(currentShootingFrame++));
                    }
                })
        );
        ShootingAnimation.setCycleCount(ShootingFrames.size());

        // Invulnerability animation
        invulnerabilityAnimation = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    sprite.setVisible(!sprite.isVisible());
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
        }
    }

    private void updateThrusterPosition() {
        if (thrusterSprite != null && isThrusting) {
            // Calculate position behind the ship based on current rotation
            double radians = Math.toRadians(rotation - 90); // Adjust angle to match ship's direction

            // Position thruster behind the ship
            Point2D thrusterOffset = new Point2D(
                    Math.cos(radians) * -20, // Move thruster back by 20 pixels
                    Math.sin(radians) * -20
            );

            // Update thruster sprite position
            thrusterSprite.setTranslateX(position.getX() + thrusterOffset.getX() - thrusterSprite.getFitWidth() / 2);
            thrusterSprite.setTranslateY(position.getY() + thrusterOffset.getY() - thrusterSprite.getFitHeight() / 2);

            // Match ship's rotation
            thrusterSprite.setRotate(rotation);
        }
    }


    private void updateShootEffectPosition(){
        if (ShootingEffect != null) {
            double radians = Math.toRadians(rotation - 90);

            // คำนวณตำแหน่งปลายกระบอกปืน
            Point2D gunOffset = new Point2D(
                    Math.cos(radians) *  20 ,
                    Math.sin(radians) *  20
            );

            // ปรับตำแหน่ง effect ให้อยู่ที่ปลายกระบอกปืน
            ShootingEffect.setTranslateX(position.getX() + gunOffset.getX() - ShootingEffect.getFitWidth() / 2);
            ShootingEffect.setTranslateY(position.getY() + gunOffset.getY() - ShootingEffect.getFitHeight() / 2);
            ShootingEffect.setRotate(rotation);
        }
    }

    public ImageView getShootEffectSprite() {
        return ShootingEffect;
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


    public void stopThrust() {
        isThrusting = false;
        if (thrusterAnimation != null) {
            thrusterAnimation.stop();
        }
        thrusterSprite.setVisible(false);
        logger.debug("Thruster animation stopped");
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
    private Timeline createShootingAnimation() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    if (currentShootingFrame < ShootingFrames.size()) {
                        ShootingEffect.setImage(ShootingFrames.get(currentShootingFrame++));
                        updateShootEffectPosition();
                    }
                })
        );

        timeline.setCycleCount(ShootingFrames.size());
        timeline.setOnFinished(e -> {
            ShootingEffect.setVisible(false);
            isShootingEffect = false;
        });

        return timeline;
    }

    private void startShootEffect() {
        if (!isShootingEffect && !isExploding && isAlive) {
            isShootingEffect = true;
            currentShootingFrame = 0;

            if (ShootingEffect != null) {
                ShootingEffect.setVisible(true);
                updateShootEffectPosition();

                if (ShootingAnimation != null) {
                    ShootingAnimation.stop();
                }

                // Only start animation if we have frames
                if (ShootingFrames != null && !ShootingFrames.isEmpty()) {
                    ShootingAnimation = createShootingAnimation();
                    ShootingAnimation.play();
                } else {
                    // Handle case where no frames are available
                    ShootingEffect.setVisible(false);
                    isShootingEffect = false;
                }
            }
        }
    }

    public se233.asteroid.model.Bullet shoot() {
        if (!isExploding && isAlive) {
            double radians = Math.toRadians(rotation - 90);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Point2D bulletPosition = position.add(direction.multiply(sprite.getBoundsInLocal().getWidth() / 2));

            // เริ่มแสดง shoot effect
            startShootEffect();
            logger.info("Shooting bullet from position: {}", bulletPosition);
            // Use the fully qualified class name to avoid confusion
            return new se233.asteroid.model.Bullet(bulletPosition, direction, false);
        }
        return null;
    }

    public se233.asteroid.model.SpecialAttack Specialshoot() {
        if (!isExploding && isAlive) {
            double radians = Math.toRadians(rotation - 90);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Point2D bulletPosition = position.add(direction.multiply(sprite.getBoundsInLocal().getWidth() / 2));
            logger.info("Shooting SpecialBullet from position: {}", bulletPosition);
            return new se233.asteroid.model.SpecialAttack(bulletPosition, direction, false);
        }
        return null;
    }

    public void hit() {
        if (!isInvulnerable && !isExploding && isAlive) {//เปิดอมตะ
            lives--;
            //explode();
            logger.info("Ship hit! Lives remaining: {}", lives);

            if (lives <= 0) {
                explode();
            } else {
                startInvulnerability();
            }
        }
    }

    public void explode() {
        isExploding = true;
        currentExplosionFrame = 0;

        // หยุดการเคลื่อนที่
        velocity = new Point2D(0, 0);

        // ปรับขนาด sprite สำหรับการระเบิด
        sprite.setFitWidth(hitRadius * 4);
        sprite.setFitHeight(hitRadius * 4);

        // ปรับตำแหน่งให้ centered
        sprite.setTranslateX(position.getX() - sprite.getFitWidth() / 2);
        sprite.setTranslateY(position.getY() - sprite.getFitHeight() / 2);

//        explosionAnimation.play();
//        explosionAnimation.setOnFinished(e -> {
        sprite.setVisible(false);  // ซ่อน sprite
        sprite.setImage(null);     // ลบรูปภาพ
        isAlive = false;           // ตั้งค่าว่าตายแล้ว
//        });

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
                })
        );
        invulnerabilityTimer.play();
        logger.debug("Invulnerability started");
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
}

