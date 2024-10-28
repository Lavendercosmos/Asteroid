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
    private static final double DEFAULT_SPEED = 2.0;
    private static final double SECOND_TIER_SPEED = 3.0;
    private static final double DEFAULT_HITBOX = 15.0;
    private static final double SECOND_TIER_HITBOX = 20.0;
    private static final double BULLET_SPEED = 5.0;
    private static final double SHOOTING_RANGE = 300.0;
    private static final double SHOOTING_COOLDOWN = 2.0; // seconds
    private static final double BEHAVIOR_CHANGE_CHANCE = 0.02;
    private static final double MIN_DISTANCE_TO_PLAYER = 100.0;

    // Sprite paths
    private static final String REGULAR_ENEMY_SPRITE = "/se233/asteroid/assets/Enemy/enemy_regular.png";
    private static final String SECOND_TIER_ENEMY_SPRITE = "/se233/asteroid/assets/Enemy/enemy_tier2.png";
    private static final String REGULAR_SHOOT_SPRITE = "/se233/asteroid/assets/Enemy/enemy_regular_shoot.png";
    private static final String SECOND_TIER_SHOOT_SPRITE = "/se233/asteroid/assets/Enemy/enemy_tier2_shoot.png";
    private static final String EXPLOSION_SPRITE = "/se233/asteroid/assets/Enemy/Enemy_Explosion.png";

    // Animation constants
    private static final int EXPLOSION_FRAME_COUNT = 9;
    private static final double EXPLOSION_FRAME_DURATION = 0.1; // seconds per frame

    // Enemy properties
    private final boolean isSecondTier;
    private final double speed;
    private double shootingCooldown;
    private EnemyBehavior currentBehavior;
    private final Random random;
    private Point2D targetPosition;
    private Map<String, Image> sprites;
    private boolean isShooting;

    // Explosion animation properties
    private Image[] explosionFrames;
    private int currentExplosionFrame;
    private double explosionTimer;
    private boolean isExploding;

    // Behavior patterns
    private enum EnemyBehavior {
        PATROL,    // Move in a pattern
        CHASE,     // Chase the player
        EVADE,     // Move away from player
        STRAFE     // Move perpendicular to player
    }

    public Enemy(Point2D position, boolean isSecondTier) {
        super(
                isSecondTier ? SECOND_TIER_ENEMY_SPRITE : REGULAR_ENEMY_SPRITE,
                position,
                isSecondTier ? SECOND_TIER_HITBOX : DEFAULT_HITBOX
        );
        this.isSecondTier = isSecondTier;
        this.speed = isSecondTier ? SECOND_TIER_SPEED : DEFAULT_SPEED;
        this.random = new Random();
        this.shootingCooldown = SHOOTING_COOLDOWN;
        this.currentBehavior = EnemyBehavior.PATROL;
        this.isExploding = false;
        this.currentExplosionFrame = 0;
        this.explosionTimer = 0;

        initializeSprites();
        initializeExplosionFrames();
        initializeVelocity();
        logger.info("Enemy created: Second Tier = {}, Position = {}", isSecondTier, position);
    }

    private void initializeSprites() {
        sprites = new HashMap<>();
        try {
            // Load normal sprites
            sprites.put("normal", new Image(getClass().getResourceAsStream(
                    isSecondTier ? SECOND_TIER_ENEMY_SPRITE : REGULAR_ENEMY_SPRITE
            )));

            // Load shooting sprites
            sprites.put("shooting", new Image(getClass().getResourceAsStream(
                    isSecondTier ? SECOND_TIER_SHOOT_SPRITE : REGULAR_SHOOT_SPRITE
            )));

            sprite.setImage(sprites.get("normal"));
        } catch (Exception e) {
            logger.error("Failed to load enemy sprites", e);
        }
    }

    private void initializeExplosionFrames() {
        explosionFrames = new Image[EXPLOSION_FRAME_COUNT];
        try {
            // Load the sprite sheet
            Image explosionSheet = new Image(getClass().getResourceAsStream(EXPLOSION_SPRITE));
            double frameWidth = explosionSheet.getWidth() / EXPLOSION_FRAME_COUNT;
            double frameHeight = explosionSheet.getHeight();

            // Create a WritableImage for each frame
            for (int i = 0; i < EXPLOSION_FRAME_COUNT; i++) {
                // Calculate the source rectangle for this frame
                double sourceX = i * frameWidth;

                // Create snapshot parameters to extract each frame
                SnapshotParameters params = new SnapshotParameters();
                params.setViewport(new Rectangle2D(sourceX, 0, frameWidth, frameHeight));

                // Create an ImageView to help with frame extraction
                ImageView frameView = new ImageView(explosionSheet);
                frameView.setViewport(new Rectangle2D(sourceX, 0, frameWidth, frameHeight));

                // Take snapshot of the frame
                explosionFrames[i] = frameView.snapshot(params, null);
            }
        } catch (Exception e) {
            logger.error("Failed to load explosion sprites", e);
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

        if (!isAlive) return;

        // Update shooting cooldown
        if (shootingCooldown > 0) {
            shootingCooldown -= 0.016; // Assuming 60 FPS
        }

        // Chance to change behavior
        if (random.nextDouble() < BEHAVIOR_CHANGE_CHANCE) {
            switchBehavior();
        }

        // Handle shooting animation
        if (isShooting) {
            sprite.setImage(sprites.get("shooting"));
            isShooting = false; // Reset after one frame
        } else {
            sprite.setImage(sprites.get("normal"));
        }

        // Update movement based on current behavior
        updateMovement();

        // Call parent update for standard functionality
        super.update();
    }

    private void updateExplosion() {
        explosionTimer += 0.016; // Assuming 60 FPS
        if (explosionTimer >= EXPLOSION_FRAME_DURATION) {
            explosionTimer = 0;
            currentExplosionFrame++;

            if (currentExplosionFrame < EXPLOSION_FRAME_COUNT) {
                sprite.setImage(explosionFrames[currentExplosionFrame]);
            } else {
                isExploding = false;
                isAlive = false;
                logger.debug("Explosion animation completed at position: {}", position);
            }
        }
    }

    public void updateAI(Point2D playerPosition) {
        if (isExploding) return;

        // Store player position for behavior updates
        this.targetPosition = playerPosition;

        // Update movement based on current behavior and player position
        double distanceToPlayer = position.distance(playerPosition);

        // Potentially shoot at player if in range
        if (distanceToPlayer <= SHOOTING_RANGE && shootingCooldown <= 0) {
            shoot(playerPosition);
            shootingCooldown = SHOOTING_COOLDOWN;
        }

        // Update behavior based on distance
        if (distanceToPlayer < MIN_DISTANCE_TO_PLAYER && currentBehavior == EnemyBehavior.CHASE) {
            currentBehavior = EnemyBehavior.EVADE;
        }
    }

    public void hit() {
        if (!isExploding) {
            isExploding = true;
            currentExplosionFrame = 0;
            explosionTimer = 0;
            velocity = new Point2D(0, 0); // Stop movement during explosion
            sprite.setImage(explosionFrames[0]);
            logger.info("Enemy hit and starting explosion animation at position: {}", position);
        }
    }

    // Existing methods remain unchanged
    private void updateMovement() {
        if (targetPosition == null) return;

        Point2D direction;
        switch (currentBehavior) {
            case CHASE:
                direction = targetPosition.subtract(position).normalize();
                velocity = direction.multiply(speed);
                break;

            case EVADE:
                direction = position.subtract(targetPosition).normalize();
                velocity = direction.multiply(speed);
                break;

            case STRAFE:
                direction = targetPosition.subtract(position);
                velocity = new Point2D(-direction.getY(), direction.getX())
                        .normalize()
                        .multiply(speed);
                break;

            case PATROL:
            default:
                if (random.nextDouble() < 0.05) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    velocity = velocity.add(new Point2D(
                            Math.cos(angle) * speed * 0.2,
                            Math.sin(angle) * speed * 0.2
                    )).normalize().multiply(speed);
                }
                break;
        }

        if (velocity.magnitude() > 0) {
            rotation = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));
        }
    }

    private void switchBehavior() {
        EnemyBehavior[] behaviors = EnemyBehavior.values();
        EnemyBehavior newBehavior;
        do {
            newBehavior = behaviors[random.nextInt(behaviors.length)];
        } while (newBehavior == currentBehavior);

        currentBehavior = newBehavior;
        logger.debug("Enemy behavior switched to: {}", currentBehavior);
    }

    public Bullet shoot(Point2D target) {
        if (isExploding) return null;

        isShooting = true;
        Point2D direction = target.subtract(position).normalize();

        // Add slight inaccuracy for regular enemies
        if (!isSecondTier) {
            double inaccuracy = (random.nextDouble() - 0.5) * 0.2; // ±0.1 radians
            double cos = Math.cos(inaccuracy);
            double sin = Math.sin(inaccuracy);
            direction = new Point2D(
                    direction.getX() * cos - direction.getY() * sin,
                    direction.getX() * sin + direction.getY() * cos
            );
        }

        // Create bullet with appropriate speed
        Bullet bullet = new Bullet(position, direction.multiply(BULLET_SPEED));
        logger.debug("Enemy fired bullet at target: {}", target);
        return bullet;
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

    public EnemyBehavior getCurrentBehavior() {
        return currentBehavior;
    }

    public boolean isInShootingRange(Point2D target) {
        return position.distance(target) <= SHOOTING_RANGE;
    }

    public double getShootingCooldown() {
        return shootingCooldown;
    }

    public void setVelocity(Point2D velocity) {
        this.velocity = velocity.normalize().multiply(speed);
    }

    public void moveTowards(Point2D target) {
        Point2D direction = target.subtract(position).normalize();
        this.velocity = direction.multiply(speed);
    }
}