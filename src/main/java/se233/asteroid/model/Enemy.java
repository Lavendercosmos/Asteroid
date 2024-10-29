package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Enemy extends Character {
    private static final Logger logger = LogManager.getLogger(Enemy.class);

    // Movement constants
    // Update movement constants for regular enemy
    private static final double DEFAULT_SPEED = 0.5;//
    private static final double SECOND_TIER_SPEED = 1.0;
    private static final double DEFAULT_HITBOX = 15.0;
    private static final double SECOND_TIER_HITBOX = 20.0;
    private static final double BULLET_SPEED = 8.0; //  // ความเร็วกระสุน

    private static final double DEFAULT_SHOOT_INTERVAL = 2.0; // Shoot every 2 seconds
    private static final double SECOND_TIER_SHOOT_INTERVAL = 1.5; // Shoot faster for second tier
    private static final double SHOOT_ACCURACY = 0.95; // 95% accuracy, add some randomness
    private static final String BULLET_SPRITE_PATH = "/se233/asteroid/assets/Enemy/Enemy_shoot.png";

    private static final double BEHAVIOR_CHANGE_CHANCE = 0.02;//
    private static final double MIN_ACCURACY = 0.98; // ความแม่นยำขั้นต่ำ (ลดการสุ่มเบี่ยงเบน)
    private static final double TRACKING_SPEED = 2.0; // ความเร็วในการติดตามเป้าหมาย
    private static final double MOVEMENT_CHANGE_INTERVAL = 2.0; // เปลี่ยนทิศทางทุก 2 วินาที
    private static final double MIN_DISTANCE_FROM_PLAYER = 150.0; // ระยะห่างขั้นต่ำจากผู้เล่น
    private static final double MAX_DISTANCE_FROM_PLAYER = 250.0; // ระยะห่างสูงสุดจากผู้เล่น

    // Sprite paths
    private static final String REGULAR_ENEMY_SPRITE = "/se233/asteroid/assets/Enemy/Enemy_ship.png";
    private static final String SECOND_TIER_ENEMY_SPRITE = "/se233/asteroid/assets/Enemy/Second_tier_enemy.png";
    private static final String REGULAR_EXPLOSION_SPRITE = "/se233/asteroid/assets/Enemy/Enemy_Explosion.png";
    private static final String SECOND_TIER_EXPLOSION_SPRITE = "/se233/asteroid/assets/Enemy/Explosion_second.png";

    // Animation constants
    private static final double EXPLOSION_FRAME_DURATION = 0.06; // Faster explosion (60ms per frame)
    private static final int EXPLOSION_FRAME_COUNT = 9; // Match the sprite sheet frame count

    private double shootTimer;
    private final double shootInterval;

    // Enemy properties
    private final boolean isSecondTier;
    private final double speed;

    private final Random random;
    private Point2D targetPosition;
    private Map<String, Image> sprites;


    private double movementTimer;
    private MovementState currentState;
    private Point2D moveDirection;

    // เพิ่ม enum สำหรับสถานะการเคลื่อนที่
    private enum MovementState {
        RANDOM_MOVE,
        MAINTAIN_DISTANCE
    }

    // Explosion properties
    private Image[] explosionFrames;
    private int currentExplosionFrame;
    private double explosionTimer;
    private boolean isExploding;
    private double explosionRotation;
    private double explosionScale;
    private Point2D explosionOffset;

    public Enemy(Point2D position, boolean isSecondTier) {
        super(
                isSecondTier ? SECOND_TIER_ENEMY_SPRITE : REGULAR_ENEMY_SPRITE,
                position,
                isSecondTier ? SECOND_TIER_HITBOX : DEFAULT_HITBOX
        );
        this.isSecondTier = isSecondTier;
        this.speed = isSecondTier ? SECOND_TIER_SPEED : DEFAULT_SPEED;
        this.shootInterval = isSecondTier ? SECOND_TIER_SHOOT_INTERVAL : DEFAULT_SHOOT_INTERVAL;
        this.shootTimer = 0;
        this.random = new Random();
        this.isExploding = false;
        this.movementTimer = 0;
        this.currentState = MovementState.RANDOM_MOVE;
        this.moveDirection = getRandomDirection();

        initializeSprites();
        initializeExplosionFrames();
        initializeVelocity();
    }


    private Point2D getRandomDirection() {
        double angle = random.nextDouble() * 2 * Math.PI;
        return new Point2D(Math.cos(angle), Math.sin(angle));
    }

    private void initializeSprites() {
        sprites = new HashMap<>();
        try {
            // Load normal state sprite
            Image normalSprite = new Image(getClass().getResourceAsStream(
                    isSecondTier ? SECOND_TIER_ENEMY_SPRITE : REGULAR_ENEMY_SPRITE
            ));
            sprites.put("normal", normalSprite);

            // Set initial sprite
            sprite.setImage(sprites.get("normal"));

            // Center the sprite properly
            sprite.setFitWidth(30);  // ปรับขนาดให้เหมาะสม
            sprite.setFitHeight(30); // ปรับขนาดให้เหมาะสม
            sprite.setPreserveRatio(true);

            // Center the sprite
            sprite.setRotate(0);
            sprite.setTranslateX(-normalSprite.getWidth() / 2);
            sprite.setTranslateY(-normalSprite.getHeight() / 2);

        } catch (Exception e) {
            logger.error("Failed to load enemy sprites: {}", e.getMessage());
        }
    }

    private void initializeExplosionFrames() {
        explosionFrames = new Image[EXPLOSION_FRAME_COUNT];
        try {
            // Load explosion sprite sheet
            String explosionPath = isSecondTier ? SECOND_TIER_EXPLOSION_SPRITE : REGULAR_EXPLOSION_SPRITE;
            Image explosionSheet = new Image(getClass().getResourceAsStream(explosionPath));

            double frameWidth = explosionSheet.getWidth() / EXPLOSION_FRAME_COUNT;
            double frameHeight = explosionSheet.getHeight();

            // Extract each frame with proper transparency
            for (int i = 0; i < EXPLOSION_FRAME_COUNT; i++) {
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(javafx.scene.paint.Color.TRANSPARENT);
                params.setViewport(new Rectangle2D(i * frameWidth, 0, frameWidth, frameHeight));

                ImageView frameView = new ImageView(explosionSheet);
                frameView.setViewport(new Rectangle2D(i * frameWidth, 0, frameWidth, frameHeight));

                // Center the frame
                frameView.setTranslateX(-frameWidth / 2);
                frameView.setTranslateY(-frameHeight / 2);

                explosionFrames[i] = frameView.snapshot(params, null);
            }
        } catch (Exception e) {
            logger.error("Failed to load explosion sprites: {}", e.getMessage());
        }
    }

    private void initializeVelocity() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double vx = Math.cos(angle) * speed;
        double vy = Math.sin(angle) * speed;
        this.velocity = new Point2D(vx, vy);
    }

    @Override
    public void update() {
        if (isExploding) {
            updateExplosion();
            return;
        }

        if (!isAlive ) return;


        updateMovement();
        updateShootTimer();
        super.update();
    }
    private void updateShootTimer() {
        if (shootTimer > 0) {
            shootTimer -= 0.016; // Assuming 60 FPS (1/60 ≈ 0.016)
        }
    }


    public EnemyBullet enemyshoot() {
        if (!isAlive || isExploding || shootTimer > 0 || targetPosition == null) {
            return null;
        }

        // Reset the shoot timer
        shootTimer = shootInterval;

        // Calculate direction to target with some randomness
        Point2D directionToTarget = targetPosition.subtract(position);

        // Add random deviation based on accuracy
        double angle = Math.atan2(directionToTarget.getY(), directionToTarget.getX());
        double randomDeviation = (1.0 - SHOOT_ACCURACY) * (random.nextDouble() - 0.5) * Math.PI;
        angle += randomDeviation;

        Point2D shootDirection = new Point2D(Math.cos(angle), Math.sin(angle)).normalize();

        // Create bullet position slightly in front of the enemy
        Point2D bulletPosition = position.add(shootDirection.multiply(getHitRadius() + 5));

        logger.debug("Enemy shooting bullet from position: {} towards direction: {}", bulletPosition, shootDirection);
        return new EnemyBullet(bulletPosition, shootDirection, true);
    }


    private void updateExplosion() {
        if (!isExploding) return;

        explosionTimer += 0.016;
        if (explosionTimer >= EXPLOSION_FRAME_DURATION) {
            explosionTimer = 0;
            currentExplosionFrame++;

            if (currentExplosionFrame < EXPLOSION_FRAME_COUNT) {
                sprite.setImage(explosionFrames[currentExplosionFrame]);
            } else {
                completeExplosion();
            }
        }
    }

    private void completeExplosion() {
        isExploding = false;
        isAlive = false;
        sprite.setScaleX(1.0);
        sprite.setScaleY(1.0);
        sprite.setRotate(0);
        logger.info("Enemy explosion completed at position: {}", position);
    }

    private void updateMovement() {
        if (targetPosition == null) return;

        double distanceToPlayer = position.distance(targetPosition);

        if (distanceToPlayer < MIN_DISTANCE_FROM_PLAYER) {
            // ถ้าอยู่ใกล้เกินไป ให้เคลื่อนที่ออกห่าง
            Point2D awayFromPlayer = position.subtract(targetPosition).normalize();
            velocity = awayFromPlayer.multiply(speed);
        } else if (distanceToPlayer > MAX_DISTANCE_FROM_PLAYER) {
            // ถ้าอยู่ไกลเกินไป ให้เคลื่อนที่เข้าใกล้
            Point2D towardsPlayer = targetPosition.subtract(position).normalize();
            velocity = towardsPlayer.multiply(speed);
        } else {
            // ถ้าอยู่ในระยะที่เหมาะสม ให้เคลื่อนที่แบบสุ่ม
            velocity = moveDirection.multiply(speed);
        }

        // หมุนยานให้หันไปทางที่กำลังเคลื่อนที่
        rotation = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));
        sprite.setRotate(rotation);
    }


    public void updateAI(Point2D playerPosition) {
        if (isExploding || !isAlive) return;

        // เก็บตำแหน่งผู้เล่นไว้สำหรับการอัพเดทพฤติกรรม
        this.targetPosition = playerPosition;

        // อัพเดทการเคลื่อนที่
        updateMovement();
    }



    public void hit() {
        if (!isExploding) {
            isExploding = true;
            currentExplosionFrame = 0;
            explosionTimer = 0;
            explosionRotation = Math.random() * 360; // Random initial rotation
            explosionScale = 1.0;
            explosionOffset = new Point2D(0, 0);
            velocity = new Point2D(0, 0); // Stop movement
            sprite.setImage(explosionFrames[0]);
            logger.info("Enemy hit and starting enhanced explosion animation at position: {}", position);
        }
    }



    // Additional getters for explosion state
    public boolean isExploding() {
        return isExploding;
    }

    public int getCurrentExplosionFrame() {
        return currentExplosionFrame;
    }

    // Existing getters remain unchanged
    public boolean isSecondTier() {
        return isSecondTier;
    }

    @Override
    public double getHitRadius() {
        return isSecondTier ? SECOND_TIER_HITBOX : DEFAULT_HITBOX;
    }


    public void setVelocity(Point2D velocity) {
        this.velocity = velocity.normalize().multiply(speed);
    }

    public void moveTowards(Point2D target) {
        Point2D direction = target.subtract(position).normalize();
        this.velocity = direction.multiply(speed);
    }
}