package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import se233.asteroid.util.SpriteSheetUtils;

public class Boss extends Character {
    private static final Logger logger = LogManager.getLogger(Boss.class);


    // Screen dimensions
    private static final double SCREEN_WIDTH = 800;
    private static final double SCREEN_HEIGHT = 600;

    // Constants for boss behavior
    private static final int BASE_HEALTH = 50;
    private static final double BASE_SPEED = 2.0;
    private static final double ATTACK_PATTERN_DURATION = 7.0;

    // Update spawn constants
    private static final double SPAWN_COOLDOWN = 5.0; // Increased cooldown to 5 seconds
    private static final double SPAWN_CHANCE = 0.5; // 50% chance to spawn on cooldown
    private static final double MIN_SPAWN_DISTANCE = 100.0;
    private static final double MAX_SPAWN_DISTANCE = 200.0;

    private static final int EXPLOSION_FRAME_COUNT = 8;
    private static final double EXPLOSION_FRAME_DURATION = 0.1; // 100ms per frame

    // Add these constants to the top of the Boss class with the other constants
    private static final int SPECIAL_ATTACK_BULLET_COUNT = 8;  // Number of bullets in special attack

    // Add new constants for reactive spawning
    private static final int SPAWN_ON_HIT_COUNT = 2; // Number of enemies to spawn when hit
    private static final double MIN_HEALTH_FOR_SPAWN = 0.1; // Minimum health percentage to spawn
    private static final double SPAWN_RADIUS = 100.0; // Distance from boss to spawn enemies

    // Add new field to track hit count
    private int hitCount = 0;
    private static final int HITS_REQUIRED_TO_SPAWN = 5;


    // Sprite paths
    private static final String BOSS_SPRITE = "/se233/asteroid/assets/Boss/Boss.png";
    private static final String BOSS_SHOOT_SPRITE = "/se233/asteroid/assets/Boss/Boss_shot.png";
    private static final String BOSS_EXPLOSION_SPRITE = "/se233/asteroid/assets/Boss/Explosion_Boss.png";

    // เพิ่มการตั้งค่าสำหรับ sprite sheet
    private static final boolean EXPLOSION_SHEET_VERTICAL = false; // แนวนอน
    private static final int ALPHA_THRESHOLD = 10; // ค่าความโปร่งใส

    // Animation properties
    private Map<String, Image[]> spriteAnimations;
    private int currentFrame = 0;
    private double frameTime = 0;
    private static final double FRAME_DURATION = 0.1;
    private boolean isShooting = false;
    private boolean isExploding = false;
    private String currentAnimationState = "normal";

    private int explosionFrame = 0;
    private double explosionTimer = 0;
    private boolean isExplodingFinal = false; // For final death explosion

    // Boss state
    private final int wave;
    private int health;
    private int maxHealth;
    private AttackPattern currentPattern;
    private final List<Bullet> activeBullets;
    private List<Enemy> spawnedEnemies;
    private final Random random;
    private Point2D playerPosition;
    private boolean isEnraged;
    private Point2D initialPosition;
    private double timeSinceLastPatternChange;
    private double timeSinceLastShot;
    private double patternTimer;
    private double horizontalSpeed;
    private double lastUpdateTime;
    private double currentRotation = 0;
    private double timeSinceLastSpawn; // Changed from timeSinceLastShot



    // Health bar components
    private StackPane healthBarGroup;
    private Rectangle healthBarFill;
    private Text healthText;
    private Text enragedText;
    private static final double HEALTH_BAR_WIDTH = 100;
    private static final double HEALTH_BAR_HEIGHT = 10;

    // Attack patterns enum
    private enum AttackPattern {
        CIRCLE,      // Boss moves in a circle while shooting
        CHASE,       // Boss chases the player
        ZIGZAG,      // Boss moves in a zigzag pattern
        SPIRAL,      // Boss moves in an expanding spiral
        RANDOM_TELEPORT  // Boss teleports randomly
    }

    private enum SpawnPattern {
        SURROUNDING, // Spawns enemies in a circle around the boss
        LINE,       // Spawns enemies in a line formation
        TRIANGLE,   // Spawns enemies in a triangle formation
        RANDOM      // Spawns enemies at random positions
    }

    public Boss(Point2D startPosition, int wave) {
        super(BOSS_SPRITE, startPosition, 50.0);
        this.wave = wave;
        this.maxHealth = BASE_HEALTH * wave;
        this.health = maxHealth;
        this.activeBullets = new CopyOnWriteArrayList<>();
        this.spawnedEnemies = new ArrayList<>();  // Initialize spawnedEnemies list
        this.random = new Random();
        this.initialPosition = startPosition;
        this.currentPattern = AttackPattern.CIRCLE;
        this.horizontalSpeed = BASE_SPEED;
        this.lastUpdateTime = System.nanoTime();
        this.timeSinceLastSpawn = SPAWN_COOLDOWN; // Set to SPAWN_COOLDOWN to spawn immediately

        initializeSpriteAnimations();
        initializeHealthBar();

        // Spawn initial enemies immediately
        spawnInitialEnemies();

        logger.info("Boss created at wave {} with {} health", wave, health);
    }

    private void spawnInitialEnemies() {
        // Don't spawn any initial enemies
        logger.info("Boss initialized without initial enemies");
    }



    private void initializeSpriteAnimations() {
        try {
            spriteAnimations = new HashMap<>();

            // โหลดเฟรมปกติ
            Image[] normalFrames = new Image[1];
            normalFrames[0] = new Image(getClass().getResourceAsStream(BOSS_SPRITE));
            spriteAnimations.put("normal", normalFrames);

            // โหลดเฟรมยิง
            Image[] shootingFrames = new Image[1];
            shootingFrames[0] = new Image(getClass().getResourceAsStream(BOSS_SHOOT_SPRITE));
            spriteAnimations.put("shooting", shootingFrames);

            // โหลดเฟรม explosion จาก sprite sheet
            List<Image> explosionFrames = SpriteSheetUtils.extractFramesWithAlpha(
                    BOSS_EXPLOSION_SPRITE,
                    EXPLOSION_FRAME_COUNT,
                    EXPLOSION_SHEET_VERTICAL,
                    ALPHA_THRESHOLD
            );
            spriteAnimations.put("explosion", explosionFrames.toArray(new Image[0]));

            logger.info("โหลดแอนิเมชันทั้งหมดสำเร็จ รวมถึง explosion sheet ที่มี {} เฟรม",
                    explosionFrames.size());
        } catch (Exception e) {
            logger.error("ไม่สามารถโหลดสไปรต์ของ boss ได้: {}", e.getMessage(), e);
        }
    }

    // เพิ่มเมธอดสำหรับตรวจสอบว่ามีแอนิเมชั่นพร้อมใช้งานหรือไม่
    private boolean hasAnimation(String state) {
        return spriteAnimations != null &&
                spriteAnimations.containsKey(state) &&
                spriteAnimations.get(state) != null &&
                spriteAnimations.get(state).length > 0;
    }


    private void initializeHealthBar() {
        healthBarGroup = new StackPane();

        // Background rectangle
        Rectangle healthBarBg = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        healthBarBg.setFill(Color.RED);

        // Health fill rectangle
        healthBarFill = new Rectangle(HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        healthBarFill.setFill(Color.GREEN);

        // Health text
        healthText = new Text();
        healthText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        healthText.setFill(Color.WHITE);

        // Enraged text
        enragedText = new Text("ENRAGED!");
        enragedText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        enragedText.setFill(Color.RED);
        enragedText.setVisible(false);

        healthBarGroup.getChildren().addAll(healthBarBg, healthBarFill, healthText, enragedText);
        updateHealthBar();
    }


    public void update(double deltaTime, Point2D playerPosition) {
        this.playerPosition = playerPosition;

        if (!isAlive()) return;

        updateTimers(deltaTime);
        updateMovementPattern();
        tryToSpawnEnemies(); // Changed from tryToShoot
        updateHealthBar();
        updateAnimation(deltaTime);
    }

    private void tryToSpawnEnemies() {
        if (timeSinceLastSpawn >= SPAWN_COOLDOWN && random.nextDouble() < SPAWN_CHANCE) {
            Enemy enemy = spawnSingleEnemy();
            if (enemy != null) {
                spawnedEnemies.add(enemy);
                timeSinceLastSpawn = 0;
                logger.info("Boss spawned a single enemy. Type: {}",
                        enemy.isSecondTier() ? "Second Tier" : "Regular");
            }
        }
    }

    // New method to spawn single enemy
    public Enemy spawnSingleEnemy() {
        // Choose random spawn pattern
        SpawnPattern pattern = getRandomSpawnPattern();
        Point2D spawnPos;
        boolean isSecondTierEnemy = isEnraged && random.nextDouble() < 0.3; // 30% chance for second-tier when enraged

        switch (pattern) {
            case SURROUNDING:
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
                spawnPos = new Point2D(
                        getPosition().getX() + Math.cos(angle) * distance,
                        getPosition().getY() + Math.sin(angle) * distance
                );
                break;

            case LINE:
                if (playerPosition != null) {
                    Point2D direction = playerPosition.subtract(getPosition()).normalize();
                    double spacing = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
                    spawnPos = getPosition().add(direction.multiply(spacing));
                } else {
                    spawnPos = getRandomSpawnPosition();
                }
                break;

            default:
                spawnPos = getRandomSpawnPosition();
                break;
        }
        // Make sure spawn position is valid
        spawnPos = validateSpawnPosition(spawnPos);
        return new Enemy(spawnPos, isSecondTierEnemy);
    }

    // Helper method to validate spawn position
    private Point2D validateSpawnPosition(Point2D pos) {
        double x = Math.max(50, Math.min(pos.getX(), SCREEN_WIDTH - 50));
        double y = Math.max(50, Math.min(pos.getY(), SCREEN_HEIGHT - 50));
        return new Point2D(x, y);
    }


    // Add method to get and clear spawned enemies
    public List<Enemy> collectSpawnedEnemies() {
        List<Enemy> enemies = new ArrayList<>(spawnedEnemies);
        spawnedEnemies.clear();
        return enemies;
    }

    private List<Enemy> spawnNormalFormation() {
        List<Enemy> enemies = new ArrayList<>();
        Enemy enemy = spawnSingleEnemy();
        if (enemy != null) {
            enemies.add(enemy);
        }
        return enemies;
    }

    private List<Enemy> spawnEnragedFormation() {
        List<Enemy> enemies = new ArrayList<>();
        Enemy enemy = spawnSingleEnemy(); // Will have higher chance of being second-tier
        if (enemy != null) {
            enemies.add(enemy);
        }
        return enemies;
    }

    private List<Enemy> spawnSurroundingEnemies(int count) {
        List<Enemy> enemies = new ArrayList<>();
        double angleStep = 360.0 / count;

        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(i * angleStep);
            double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
            Point2D spawnPos = new Point2D(
                    getPosition().getX() + Math.cos(angle) * distance,
                    getPosition().getY() + Math.sin(angle) * distance
            );

            enemies.add(new Enemy(spawnPos, false));
        }
        return enemies;
    }

    private List<Enemy> spawnLineFormation(int count) {
        List<Enemy> enemies = new ArrayList<>();
        double spacing = 50; // Space between enemies

        Point2D direction = playerPosition != null ?
                playerPosition.subtract(getPosition()).normalize() :
                new Point2D(1, 0);

        for (int i = 0; i < count; i++) {
            Point2D offset = direction.multiply(spacing * i);
            Point2D spawnPos = getPosition().add(offset);
            enemies.add(new Enemy(spawnPos, false));
        }
        return enemies;
    }

    private List<Enemy> spawnTriangleFormation(int count) {
        List<Enemy> enemies = new ArrayList<>();
        double radius = MIN_SPAWN_DISTANCE;
        double angleStep = 120; // 3 points for triangle

        for (int i = 0; i < Math.min(count, 3); i++) {
            double angle = Math.toRadians(i * angleStep);
            Point2D spawnPos = new Point2D(
                    getPosition().getX() + Math.cos(angle) * radius,
                    getPosition().getY() + Math.sin(angle) * radius
            );
            enemies.add(new Enemy(spawnPos, false));
        }
        return enemies;
    }

    private List<Enemy> spawnRandomEnemies(int count) {
        List<Enemy> enemies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
            Point2D spawnPos = new Point2D(
                    getPosition().getX() + Math.cos(angle) * distance,
                    getPosition().getY() + Math.sin(angle) * distance
            );
            enemies.add(new Enemy(spawnPos, false));
        }
        return enemies;
    }

    private List<Enemy> spawnFormationWithSecondTier(SpawnPattern pattern, int count) {
        List<Enemy> enemies = new ArrayList<>();
        switch (pattern) {
            case SURROUNDING:
                double angleStep = 360.0 / count;
                for (int i = 0; i < count; i++) {
                    double angle = Math.toRadians(i * angleStep);
                    Point2D spawnPos = new Point2D(
                            getPosition().getX() + Math.cos(angle) * MAX_SPAWN_DISTANCE,
                            getPosition().getY() + Math.sin(angle) * MAX_SPAWN_DISTANCE
                    );
                    enemies.add(new Enemy(spawnPos, true)); // Second-tier enemies
                }
                break;
            default:
                // For other patterns, just spawn random second-tier enemies
                for (int i = 0; i < count; i++) {
                    Point2D spawnPos = getRandomSpawnPosition();
                    enemies.add(new Enemy(spawnPos, true));
                }
                break;
        }
        return enemies;
    }

    private Point2D getRandomSpawnPosition() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
        return new Point2D(
                getPosition().getX() + Math.cos(angle) * distance,
                getPosition().getY() + Math.sin(angle) * distance
        );
    }

    private SpawnPattern getRandomSpawnPattern() {
        return SpawnPattern.values()[random.nextInt(SpawnPattern.values().length)];
    }

    private void updateTimers(double deltaTime) {
        timeSinceLastPatternChange += deltaTime;
        timeSinceLastShot += deltaTime;
        timeSinceLastSpawn += deltaTime;  // Update spawn timer
        patternTimer += deltaTime;

        if (timeSinceLastPatternChange >= ATTACK_PATTERN_DURATION) {
            changePattern();
            timeSinceLastPatternChange = 0;
            patternTimer = 0;
        }
    }

    private void updateMovementPattern() {
        if (isEnraged) {
            chasePlayer();
            return;
        }

        switch (currentPattern) {
            case CIRCLE -> moveInCircle();
            case CHASE -> chasePlayer();
            case ZIGZAG -> moveInZigzag();
            case SPIRAL -> moveInSpiral();
            case RANDOM_TELEPORT -> handleTeleport();
        }
    }

    private void moveInCircle() {
        double radius = 150;
        double speed = isEnraged ? BASE_SPEED * 1.5 : BASE_SPEED;
        double angle = patternTimer * speed;

        setPosition(new Point2D(
                initialPosition.getX() + Math.cos(angle) * radius,
                initialPosition.getY() + Math.sin(angle) * radius
        ));
    }

    private void chasePlayer() {
        if (playerPosition == null) return;

        Point2D direction = playerPosition.subtract(getPosition()).normalize();
        double speed = isEnraged ? BASE_SPEED * 1.5 : BASE_SPEED;
        setPosition(getPosition().add(direction.multiply(speed)));
    }


    private void moveInZigzag() {
        double amplitude = 100;
        double frequency = isEnraged ? 3.0 : 2.0;

        setPosition(new Point2D(
                getPosition().getX() + horizontalSpeed,
                initialPosition.getY() + amplitude * Math.sin(patternTimer * frequency)
        ));

        if (getPosition().getX() < 0 || getPosition().getX() > SCREEN_WIDTH) {
            horizontalSpeed = -horizontalSpeed;
        }
    }

    private void moveInSpiral() {
        double expandingRadius = 50 + patternTimer * 20;
        double angle = patternTimer * 3;

        setPosition(new Point2D(
                initialPosition.getX() + Math.cos(angle) * expandingRadius,
                initialPosition.getY() + Math.sin(angle) * expandingRadius
        ));

        if (expandingRadius > 200) {
            patternTimer = 0;
        }
    }

    private void handleTeleport() {
        if (patternTimer >= 1.0) {
            setPosition(getRandomPosition());
            patternTimer = 0;
        }
    }

    private void updateHealthBar() {
        if (healthBarGroup == null) return;

        double healthPercent = getHealthPercentage();
        healthBarFill.setWidth(HEALTH_BAR_WIDTH * healthPercent);


        healthBarGroup.setTranslateX(getPosition().getX() - HEALTH_BAR_WIDTH / 2);
        healthBarGroup.setTranslateY(getPosition().getY() - 40);

        enragedText.setVisible(isEnraged);
    }

    private void updateAnimation(double deltaTime) {
        frameTime += deltaTime;

        if (isExplodingFinal) {
            explosionTimer += deltaTime;
            if (explosionTimer >= EXPLOSION_FRAME_DURATION) {
                explosionTimer = 0;
                explosionFrame++;

                Image[] explosionFrames = spriteAnimations.get("explosion");
                if (explosionFrames != null && explosionFrame < explosionFrames.length) {
                    setImage(explosionFrames[explosionFrame]);

                    // ขยายขนาดตามความคืบหน้าของ explosion
                    double baseScale = 1.0;
                    double explosionProgress = (double) explosionFrame / EXPLOSION_FRAME_COUNT;
                    double scale = baseScale + (explosionProgress * 0.5); // ใหญ่ขึ้น 50% ตอนจบ
                    sprite.setFitWidth(100 * scale);
                    sprite.setFitHeight(100 * scale);

                    // เพิ่มเอฟเฟกต์จางหายตอนท้าย
                    if (explosionFrame >= EXPLOSION_FRAME_COUNT * 0.7) {
                        double opacity = 1.0 - ((explosionFrame - (EXPLOSION_FRAME_COUNT * 0.7))
                                / (EXPLOSION_FRAME_COUNT * 0.3));
                        sprite.setOpacity(Math.max(0, opacity));
                    }

                } else if (explosionFrame >= EXPLOSION_FRAME_COUNT) {
                    setAlive(false);
                    cleanup();
                }
                return;
            }
        }

        // อัปเดตแอนิเมชันปกติ
        if (frameTime >= FRAME_DURATION) {
            frameTime = 0;
            String newState = determineAnimationState();

            if (!currentAnimationState.equals(newState)) {
                currentAnimationState = newState;
                currentFrame = 0;
                sprite.setOpacity(1.0); // รีเซ็ตความโปร่งใสสำหรับแอนิเมชันปกติ
            }

            Image[] frames = spriteAnimations.get(currentAnimationState);
            if (frames != null) {
                currentFrame = (currentFrame + 1) % frames.length;
                setImage(frames[currentFrame]);

                // ขนาดปกติสำหรับแอนิเมชันที่ไม่ใช่ explosion
                if (!isExplodingFinal) {
                    sprite.setFitWidth(100);
                    sprite.setFitHeight(100);
                }
            }

            isShooting = false;

            // จัดการแอนิเมชัน explosion ชั่วคราวตอนโดนโจมตี
            if (isExploding && !isExplodingFinal) {
                Image[] explosionFrames = spriteAnimations.get("explosion");
                if (explosionFrames != null) {
                    int hitFrame = currentFrame % 3; // ใช้แค่ 3 เฟรมแรกสำหรับเอฟเฟกต์โดนโจมตี
                    setImage(explosionFrames[hitFrame]);
                    sprite.setOpacity(0.8);
                }

                if (currentFrame >= 2) {
                    isExploding = false;
                    sprite.setOpacity(1.0);
                }
            }
        }
    }

    private String determineAnimationState() {
        if (isExploding && hasAnimation("explosion")) {
            return "explosion";
        } else if (isShooting && hasAnimation("shooting")) {
            return "shooting";
        } else if (hasAnimation("normal")) {
            return "normal";
        }
        // ถ้าไม่มีแอนิเมชั่นใดๆ ให้ล็อกข้อผิดพลาด
        logger.error("No animations available!");
        return "normal"; // ใช้สถานะปกติเป็นค่าเริ่มต้น
    }

    private void changePattern() {
        AttackPattern[] patterns = AttackPattern.values();
        AttackPattern newPattern;

        do {
            newPattern = patterns[random.nextInt(patterns.length)];
        } while (newPattern == currentPattern);

        currentPattern = newPattern;
        logger.info("Boss changing to attack pattern: {}", currentPattern);

        // Reset pattern-specific variables
        patternTimer = 0;
        if (currentPattern == AttackPattern.CIRCLE || currentPattern == AttackPattern.SPIRAL) {
            initialPosition = getPosition();
        }
    }

    private Point2D getRandomPosition() {
        double margin = 50;  // Keep boss away from screen edges
        return new Point2D(
                margin + random.nextDouble() * (SCREEN_WIDTH - 2 * margin),
                margin + random.nextDouble() * (SCREEN_HEIGHT - 2 * margin)
        );
    }

    public void hit(int damage) {
        takeDamage(damage);
    }

    private void takeDamage(int damage) {
        int previousHealth = health;
        health = Math.max(0, health - damage);
        double healthPercentage = getHealthPercentage();

        // Only show hit animation if not dying
        if (!isExplodingFinal) {
            isExploding = true;
        }

        // Increment hit counter and check for spawning
        hitCount++;
        if (hitCount >= HITS_REQUIRED_TO_SPAWN) {
            // Reset hit counter
            hitCount = 0;

            // Spawn enemies if health is above minimum threshold
            if (healthPercentage > MIN_HEALTH_FOR_SPAWN && previousHealth > health) {
                spawnEnemiesOnHit();
            }
        }

        if (health <= maxHealth * 0.3 && !isEnraged) {
            enterEnragedState();
        }

        if (health <= 0 && !isExplodingFinal) {
            die();
        }

        updateHealthBar();
        logger.info("Boss took {} damage. Current health: {}/{}. Hits until spawn: {}",
                damage, health, maxHealth, HITS_REQUIRED_TO_SPAWN - hitCount);
    }

    private void spawnEnemiesOnHit() {
        if (health > 0) {
            // Spawn 2-3 enemies when hit counter reaches threshold
            int enemiesToSpawn = 2 + random.nextInt(2); // Random between 2-3 enemies

            for (int i = 0; i < enemiesToSpawn; i++) {
                Enemy enemy = spawnSingleEnemy();
                if (enemy != null) {
                    spawnedEnemies.add(enemy);
                    logger.info("Boss spawned enemy after {} hits. Type: {}",
                            HITS_REQUIRED_TO_SPAWN,
                            enemy.isSecondTier() ? "Second Tier" : "Regular");
                }
            }
        }
    }

    private void enterEnragedState() {
        isEnraged = true;
        logger.info("Boss entering enraged state!");

        // Apply enraged state effects
        horizontalSpeed *= 1.5;

        // Visual feedback
        if (enragedText != null) {
            enragedText.setVisible(true);
        }
    }

    private void die() {
        isExplodingFinal = true;
        explosionFrame = 0;
        explosionTimer = 0;
        isExploding = false; // Stop regular explosion animation

        // Hide health bar immediately
        if (healthBarGroup != null) {
            healthBarGroup.setVisible(false);
        }

        logger.info("Boss starting death animation at wave {}", wave);
    }

    public void cleanup() {
        if (healthBarGroup != null && healthBarGroup.getParent() instanceof Pane) {
            ((Pane) healthBarGroup.getParent()).getChildren().remove(healthBarGroup);
        }
        activeBullets.clear();
    }

    // Add this method to the Boss class
    public Bullet[] shootSpecialAttack() {
        if (!isAlive() || isExplodingFinal) {
            return new Bullet[0];
        }

        Bullet[] bullets = new Bullet[SPECIAL_ATTACK_BULLET_COUNT];
        double angleStep = 360.0 / SPECIAL_ATTACK_BULLET_COUNT;

        // Set shooting animation state
        isShooting = true;

        // Create bullets in a circular pattern
        for (int i = 0; i < SPECIAL_ATTACK_BULLET_COUNT; i++) {
            double angle = Math.toRadians(i * angleStep + currentRotation);
            Point2D direction = new Point2D(Math.cos(angle), Math.sin(angle));

            // Use the available Bullet constructor
            // Note: true parameter indicates this is an enemy bullet
            bullets[i] = new Bullet(getPosition(), direction, true);
        }

        // Rotate the pattern for next time
        currentRotation += 15;
        if (currentRotation >= 360) {
            currentRotation = 0;
        }

        logger.info("Boss fired special attack with {} bullets", SPECIAL_ATTACK_BULLET_COUNT);
        return bullets;
    }

    public void addToPane(Pane pane) {
        if (healthBarGroup != null && !pane.getChildren().contains(healthBarGroup)) {
            pane.getChildren().add(healthBarGroup);
        }
    }

    // Getters and Setters

    public List<Bullet> getBullets() {
        return activeBullets;
    }

    public double getHealthPercentage() {
        return (double) health / maxHealth;
    }

    public boolean isEnraged() {
        return isEnraged;
    }

    /**
     * Gets the current wave number.
     */
    public int getWave() {
        return wave;
    }

    /**
     * Gets the current health value.
     */
    public int getHealth() {
        return health;
    }

    /**
     * Gets the maximum health value.
     */
    public int getMaxHealth() {
        return maxHealth;
    }

    public AttackPattern getCurrentPattern() {
        return currentPattern;
    }

    public boolean isExploding() {
        return isExploding;
    }
}