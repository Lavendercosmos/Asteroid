package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import static se233.asteroid.util.SpriteSheetUtils.extractFrames;

public class Boss extends Character {
    private static final Logger logger = LogManager.getLogger(Boss.class);

    // Constants
    private static final int BASE_HEALTH = 200;
    private static final double BASE_SPEED = 3.0;
    private static final double BULLET_SPEED = 5.0;
    private static final double ATTACK_PATTERN_DURATION = 5.0;
    private static final double BULLET_COOLDOWN = 0.5; // seconds
    private static final int RADIAL_BULLET_COUNT = 8;

    // Sprite paths - Updated to use consistent lowercase paths
    private static final String BOSS_NORMAL_SPRITE = "/se233/asteroid/assets/Boss/Boss.png";
    private static final String BOSS_SHOOT_SPRITE = "/se233/asteroid/assets/Boss/Boss_shot.png";  // Changed from boss_shoot.png
    private static final String BOSS_EXPLOSION_SPRITE = "/se233/asteroid/assets/Boss/Explosion_Boss.png";

    // Rest of the class remains unchanged...
    private Map<String, Image[]> spriteAnimations;
    private int currentFrame = 0;
    private double frameTime = 0;
    private static final double FRAME_DURATION = 0.1; // 100ms per frame
    private boolean isShooting = false;
    private boolean isExploding = false;
    private String currentAnimationState = "normal";

    // Boss properties
    private final int wave;
    private int health;
    private int maxHealth;
    private AttackPattern currentPattern;
    private final List<Bullet> activeBullets;
    private final Random random;

    // State tracking
    private double timeSinceLastPatternChange;
    private double timeSinceLastShot;
    private double patternTimer;
    private boolean isEnraged;
    private Point2D initialPosition;
    private double horizontalSpeed;  // For zigzag pattern
    private double lastUpdateTime;

    // Health bar components
    private static final double HEALTH_BAR_WIDTH = 100;
    private static final double HEALTH_BAR_HEIGHT = 10;
    private StackPane healthBarGroup;
    private Rectangle healthBarBackground;
    private Rectangle healthBarFill;
    private Text healthText;
    private Text enragedText;

    // Enum for attack patterns
    private enum AttackPattern {
        CIRCLE,
        CHASE,
        ZIGZAG,
        SPIRAL,
        RANDOM_TELEPORT
    }

    public Boss(Point2D startPosition, int wave) {
        super(BOSS_NORMAL_SPRITE, startPosition, 50.0);

        try {
            this.wave = wave;
            this.maxHealth = BASE_HEALTH * wave;
            this.health = maxHealth;
            this.activeBullets = new CopyOnWriteArrayList<>();
            this.random = new Random();
            this.initialPosition = startPosition;
            this.currentPattern = AttackPattern.CIRCLE;
            this.timeSinceLastPatternChange = 0;
            this.timeSinceLastShot = 0;
            this.patternTimer = 0;
            this.isEnraged = false;
            this.horizontalSpeed = BASE_SPEED;
            this.lastUpdateTime = System.nanoTime();

            // Initialize health bar
            initializeHealthBar();

            initializeSpriteAnimations();

            logger.info("Boss created at position: {} with health: {}", startPosition, health);
        } catch (Exception e) {
        logger.error("Failed to initialize boss: {}", e.getMessage());
        throw new RuntimeException("Failed to initialize boss", e);
    }
}

    private void initializeSpriteAnimations() {
        spriteAnimations = new HashMap<>();
        try {
            logger.debug("Attempting to load boss sprites from paths:");
            logger.debug("Normal: {}", BOSS_NORMAL_SPRITE);
            logger.debug("Shoot: {}", BOSS_SHOOT_SPRITE);
            logger.debug("Explosion: {}", BOSS_EXPLOSION_SPRITE);

            // Load normal boss sprite
            Image bossNormal = new Image(getClass().getResourceAsStream(BOSS_NORMAL_SPRITE));
            spriteAnimations.put("normal", new Image[]{bossNormal});

            // Load shooting animation frames
            // Note: Boss_shot.png appears to be a single image, not a sprite sheet
            Image shootSprite = new Image(getClass().getResourceAsStream(BOSS_SHOOT_SPRITE));
            spriteAnimations.put("shoot", new Image[]{shootSprite});

            // Load explosion animation frames
            // The explosion sprite appears to have 8 frames
            List<Image> explosionFrames = extractFrames(BOSS_EXPLOSION_SPRITE, 8, false);
            spriteAnimations.put("explosion", explosionFrames.toArray(new Image[0]));

            logger.info("Successfully loaded all boss sprites");

        } catch (Exception e) {
            logger.error("Failed to load boss sprites: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load boss sprites", e);
        }
    }

    private void initializeHealthBar() {
        // Use StackPane instead of Group
        healthBarGroup = new StackPane();

        // Create background bar
        healthBarBackground = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        healthBarBackground.setFill(Color.rgb(50, 50, 50, 0.8));
        healthBarBackground.setArcWidth(5);
        healthBarBackground.setArcHeight(5);

        // Create health fill bar
        healthBarFill = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        healthBarFill.setFill(Color.GREEN);
        healthBarFill.setArcWidth(5);
        healthBarFill.setArcHeight(5);

        // Create health percentage text
        healthText = new Text(String.format("%.0f%%", getHealthPercentage() * 100));
        healthText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        healthText.setFill(Color.WHITE);
        healthText.setTranslateX(HEALTH_BAR_WIDTH / 2 - 15);
        healthText.setTranslateY(HEALTH_BAR_HEIGHT - 1);

        // Create enraged text
        enragedText = new Text("ENRAGED");
        enragedText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        enragedText.setFill(Color.RED);
        enragedText.setTranslateX(HEALTH_BAR_WIDTH / 2 - 30);
        enragedText.setTranslateY(-5);
        enragedText.setVisible(false);

        // Add all components to the StackPane
        healthBarGroup.getChildren().addAll(
                healthBarBackground,
                healthBarFill,
                healthText,
                enragedText
        );

        // Position health bar above the boss sprite
        updateHealthBarPosition();

        // Add health bar group to the scene
        if (getSprite().getParent() instanceof Pane) {
            ((Pane) getSprite().getParent()).getChildren().add(healthBarGroup);
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        isExploding = true;
        currentFrame = 0;

        logger.info("Boss hit! Health remaining: {}/{}", health, maxHealth);

        // Update health bar immediately when hit
        if (health > 0) {
            updateHealthBar();
        } else {
            setAlive(false);
            if (healthBarGroup != null) {
                healthBarGroup.setVisible(false);
            }
            logger.info("Boss defeated!");
        }
    }

    private void updateHealthBarPosition() {
        // Position health bar above the boss
        double xOffset = -HEALTH_BAR_WIDTH / 2;
        double yOffset = -getSprite().getBoundsInLocal().getHeight() / 2 - HEALTH_BAR_HEIGHT - 10;

        healthBarGroup.setTranslateX(getPosition().getX() + xOffset);
        healthBarGroup.setTranslateY(getPosition().getY() + yOffset);
    }

    private void updateHealthBar() {
        // Update health bar fill width based on current health
        double healthPercentage = getHealthPercentage();
        healthBarFill.setWidth(HEALTH_BAR_WIDTH * healthPercentage);

        // Update health text
        healthText.setText(String.format("%.0f%%", healthPercentage * 100));

        // Update health bar color based on health and enraged state
        if (isEnraged) {
            healthBarFill.setFill(Color.RED);
            enragedText.setVisible(true);
        } else if (healthPercentage > 0.6) {
            healthBarFill.setFill(Color.GREEN);
            enragedText.setVisible(false);
        } else if (healthPercentage > 0.3) {
            healthBarFill.setFill(Color.YELLOW);
            enragedText.setVisible(false);
        } else {
            healthBarFill.setFill(Color.ORANGE);
            enragedText.setVisible(false);
        }

        // Update position to follow boss
        updateHealthBarPosition();
    }

    public void hit(int damage) {
        takeDamage(damage);
    }



    @Override
    public void update() {
        super.update();

        if (isAlive()) {
            updateHealthBar();
        } else if (healthBarGroup != null) {
            // Remove health bar when boss is defeated
            healthBarGroup.setVisible(false);
        }
    }

    @Override
    public void setPosition(Point2D position) {
        super.setPosition(position);
        if (healthBarGroup != null) {
            updateHealthBarPosition();
        }
    }


    // Add cleanup method
    public void cleanup() {
        if (healthBarGroup != null && healthBarGroup.getParent() instanceof Pane) {
            ((Pane) healthBarGroup.getParent()).getChildren().remove(healthBarGroup);
        }
    }

    public void update(double deltaTime, Point2D playerPosition) {
        // Calculate delta time
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0; // Convert to seconds
        lastUpdateTime = currentTime;

        // Call parent update first
        super.update();

        if (!isAlive()) return;

        // Update animation frame time
        frameTime += deltaTime;
        if (frameTime >= FRAME_DURATION) {
            frameTime = 0;
            updateAnimation();
        }

        // Update timers
        timeSinceLastPatternChange += deltaTime;
        timeSinceLastShot += deltaTime;
        patternTimer += deltaTime;

        // Check if it's time to change pattern
        if (timeSinceLastPatternChange >= ATTACK_PATTERN_DURATION) {
            changePattern();
            timeSinceLastPatternChange = 0;
            patternTimer = 0;
        }

        // Update movement based on current pattern
        updateMovement();

        // Update bullets and remove out-of-bounds ones
        updateBullets();

        // Check if should enter enraged state
        checkEnragedState();

        // Try to fire based on pattern and cooldown
        if (timeSinceLastShot >= BULLET_COOLDOWN) {
            fireBasedOnPattern();
        }
    }

    private void updateAnimation() {
        if (isExploding) {
            Image[] explosionFrames = spriteAnimations.get("explosion");
            currentFrame = (currentFrame + 1) % explosionFrames.length;
            getSprite().setImage(explosionFrames[currentFrame]);
            currentAnimationState = "explosion";

            if (currentFrame == explosionFrames.length - 1) {
                isExploding = false;
                getSprite().setImage(spriteAnimations.get("normal")[0]);
                currentAnimationState = "normal";
            }
        } else if (isShooting) {
            Image[] shootFrames = spriteAnimations.get("shoot");
            currentFrame = (currentFrame + 1) % shootFrames.length;
            getSprite().setImage(shootFrames[currentFrame]);
            currentAnimationState = "shoot";

            if (currentFrame == shootFrames.length - 1) {
                isShooting = false;
                getSprite().setImage(spriteAnimations.get("normal")[0]);
                currentAnimationState = "normal";
            }
        }
    }

    private void updateMovement() {
        switch (currentPattern) {
            case CIRCLE:
                updateCirclePattern();
                break;
            case CHASE:
                updateChasePattern();
                break;
            case ZIGZAG:
                updateZigzagPattern();
                break;
            case SPIRAL:
                updateSpiralPattern();
                break;
            case RANDOM_TELEPORT:
                updateTeleportPattern();
                break;
        }
    }

    private void updateCirclePattern() {
        double radius = 150;
        double speed = isEnraged ? BASE_SPEED * 1.5 : BASE_SPEED;
        double angle = patternTimer * speed;

        setPosition(new Point2D(
                initialPosition.getX() + Math.cos(angle) * radius,
                initialPosition.getY() + Math.sin(angle) * radius
        ));
    }

    private void updateChasePattern() {
        // Chase towards center of screen
        Point2D center = new Point2D(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
        Point2D direction = center.subtract(getPosition()).normalize();
        double speed = isEnraged ? BASE_SPEED * 1.5 : BASE_SPEED;
        setVelocity(direction.multiply(speed));
    }

    private void updateZigzagPattern() {
        double amplitude = 100;
        double frequency = isEnraged ? 3.0 : 2.0;

        setPosition(new Point2D(
                getPosition().getX() + horizontalSpeed,
                initialPosition.getY() + amplitude * Math.sin(patternTimer * frequency)
        ));

        // Reverse direction at screen bounds
        if (getPosition().getX() < 0 || getPosition().getX() > SCREEN_WIDTH) {
            horizontalSpeed = -horizontalSpeed;
        }
    }

    private void updateSpiralPattern() {
        double expandingRadius = 50 + patternTimer * 20;
        double angle = patternTimer * 3;

        setPosition(new Point2D(
                initialPosition.getX() + Math.cos(angle) * expandingRadius,
                initialPosition.getY() + Math.sin(angle) * expandingRadius
        ));

        // Reset spiral when radius gets too large
        if (expandingRadius > 200) {
            patternTimer = 0;
        }
    }

    private void updateTeleportPattern() {
        if (patternTimer >= 1.0) { // Teleport every second
            setPosition(getRandomPosition());
            patternTimer = 0;
        }
    }

    private void updateBullets() {
        activeBullets.removeIf(bullet -> {
            bullet.update();
            Point2D pos = bullet.getPosition();
            return pos.getX() < 0 || pos.getX() > SCREEN_WIDTH ||
                    pos.getY() < 0 || pos.getY() > SCREEN_HEIGHT;
        });
    }

    public Bullet[] shootSpecialAttack() {
        isShooting = true;
        currentFrame = 0;
        return isEnraged ? shootEnragedSpecialAttack() : shootNormalSpecialAttack();
    }

    private Bullet[] shootNormalSpecialAttack() {
        Bullet[] bullets = new Bullet[RADIAL_BULLET_COUNT];
        for (int i = 0; i < RADIAL_BULLET_COUNT; i++) {
            double angle = i * (360.0 / RADIAL_BULLET_COUNT);
            double radians = Math.toRadians(angle);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            // สร้าง bullet แบบเป็นกระสุนของศัตรู (isEnemyBullet = true)
            bullets[i] = new Bullet(getPosition(), direction.multiply(BULLET_SPEED), true);
            activeBullets.add(bullets[i]);
        }
        return bullets;
    }

    private Bullet[] shootEnragedSpecialAttack() {
        Bullet[] bullets = new Bullet[RADIAL_BULLET_COUNT * 2];
        for (int i = 0; i < RADIAL_BULLET_COUNT * 2; i++) {
            double angle = i * (360.0 / (RADIAL_BULLET_COUNT * 2));
            double radians = Math.toRadians(angle);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            bullets[i] = new Bullet(getPosition(), direction.multiply(BULLET_SPEED * 1.5), true);
            activeBullets.add(bullets[i]);
        }
        return bullets;
    }

    private void fireBasedOnPattern() {
        isShooting = true;
        currentFrame = 0;

        if (isEnraged) {
            fireRadialBullets();
            fireTargetedBullets();
        } else {
            switch (currentPattern) {
                case CIRCLE:
                case SPIRAL:
                    fireRadialBullets();
                    break;
                case CHASE:
                case ZIGZAG:
                    fireTargetedBullets();
                    break;
                case RANDOM_TELEPORT:
                    fireRandomBullets();
                    break;
            }
        }

        timeSinceLastShot = 0;
    }

    private void fireRadialBullets() {
        for (int i = 0; i < RADIAL_BULLET_COUNT; i++) {
            double angle = i * (360.0 / RADIAL_BULLET_COUNT);
            double radians = Math.toRadians(angle);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Bullet bullet = new Bullet(getPosition(), direction.multiply(BULLET_SPEED), true);
            activeBullets.add(bullet);
        }
    }

    private void fireTargetedBullets() {
        Point2D center = new Point2D(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
        Point2D direction = center.subtract(getPosition()).normalize();
        Bullet bullet = new Bullet(getPosition(), direction.multiply(BULLET_SPEED), true);
        activeBullets.add(bullet);
    }

    private void fireRandomBullets() {
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * 360;
            double radians = Math.toRadians(angle);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Bullet bullet = new Bullet(getPosition(), direction.multiply(BULLET_SPEED), true);
            activeBullets.add(bullet);
        }
    }

    private void changePattern() {
        AttackPattern[] patterns = AttackPattern.values();
        AttackPattern newPattern;
        do {
            newPattern = patterns[random.nextInt(patterns.length)];
        } while (newPattern == currentPattern);

        currentPattern = newPattern;
        initialPosition = getPosition();
        logger.info("Boss changing to attack pattern: {}", currentPattern);
    }

    private void checkEnragedState() {
        boolean shouldBeEnraged = health <= maxHealth * 0.3;
        if (shouldBeEnraged != isEnraged) {
            isEnraged = shouldBeEnraged;
            getSprite().setImage(spriteAnimations.get("normal")[0]); // Reset to normal sprite first
            logger.info("Boss {} enraged state", isEnraged ? "entered" : "left");
        }
    }


    private Point2D getRandomPosition() {
        return new Point2D(
                random.nextDouble() * (SCREEN_WIDTH - 100) + 50,
                random.nextDouble() * (SCREEN_HEIGHT - 100) + 50
        );
    }

    // Getters
    public List<Bullet> getBullets() {
        return activeBullets;
    }

    public int getWave() {
        return wave;
    }

    public double getHealthPercentage() {
        return (double) health / maxHealth;
    }

    public boolean isEnraged() {
        return isEnraged;
    }

    public boolean isExploding() {
        return isExploding;
    }

    public boolean isShooting() {
        return isShooting;
    }
}