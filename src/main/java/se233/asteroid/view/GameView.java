package se233.asteroid.view;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;
import se233.asteroid.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.asteroid.model.Character;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameView extends Pane {
    private static final Logger logger = LogManager.getLogger(GameView.class);

    // Game constants
    public static final double DEFAULT_WIDTH = 800;
    public static final double DEFAULT_HEIGHT = 600;
    private static final long BULLET_COOLDOWN = 250_000_000L; // 250ms
    private static final int INITIAL_ENEMIES = 2;
    private static final double ENEMY_SPAWN_CHANCE = 0.01; // 1% chance per frame
    private static final int MAX_ENEMIES = 3;
    private static final int POINTS_REGULAR_ENEMY = 100;
    private static final int POINTS_SECOND_TIER_ENEMY = 250;

    // Game components
    private final GameStage gameStage;
    private final List<Character> gameObjects;
    private final List<Bullet> bullets;
    private final List<Enemy> enemies;
    private PlayerShip player;
    private Boss boss;

    // Game state
    private boolean isGameStarted;
    private boolean isPaused;
    private double currentWidth;
    private double currentHeight;
    private int currentWave;
    private int currentScore;
    private long lastBulletTime;
    private final Random random;

    public GameView() {
        // Initialize collections
        this.gameObjects = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.enemies = new CopyOnWriteArrayList<>();
        this.random = new Random();

        // Setup stage and size
        this.gameStage = new GameStage();
        this.currentWidth = DEFAULT_WIDTH;
        this.currentHeight = DEFAULT_HEIGHT;
        getChildren().add(gameStage);

        // Initialize state
        this.isGameStarted = false;
        this.isPaused = false;
        this.currentWave = 1;
        this.currentScore = 0;

        setupButtonHandlers();
        setupGameLoop();

        logger.info("GameView initialized");
    }

    private void setupButtonHandlers() {
        gameStage.getStartButton().setOnAction(e -> startGame());
        gameStage.getRestartButton().setOnAction(e -> resetGame());
        gameStage.getResumeButton().setOnAction(e -> resumeGame());
    }

    private void setupGameLoop() {
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPaused && isGameStarted) {
                    updateGame();
                    checkCollisions();
                    checkWaveCompletion();
                    spawnNewEnemies();
                }
            }
        };
        gameLoop.start();
    }

    private void updateGame() {
        // Update player
        if (player != null && player.isAlive()) {
            player.update();
            wrapAround(player);
        }

        // Update enemies
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.update();
                enemy.updateAI(player.getPosition());
                wrapAround(enemy);

                // Enemy shooting logic
                if (enemy.isInShootingRange(player.getPosition()) &&
                        enemy.getShootingCooldown() <= 0) {
                    Bullet bullet = enemy.shoot(player.getPosition());
                    if (bullet != null) {
                        bullets.add(bullet);
                        gameStage.addBullet(bullet);
                    }
                }
            }
        }

        // Update bullets
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            bullet.update();
            if (isOffScreen(bullet.getPosition())) {
                gameStage.removeBullet(bullet);
                bulletIter.remove();
            }
        }

        // Update other game objects
        for (Character obj : gameObjects) {
            if (obj.isAlive()) {
                obj.update();
                wrapAround(obj);
            }
        }

        // Update boss
        if (boss != null && boss.isAlive()) {
            boss.update();
            wrapAround(boss);
        }
    }


    private void checkCollisions() {
        if (player == null || !player.isAlive() || player.isInvulnerable()) return;

        // Check bullet collisions
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            boolean bulletHit = false;

            // Skip enemy bullets hitting enemies
            if (bullet.isEnemyBullet()) {
                // Check if enemy bullet hits player
                if (bullet.collidesWith(player)) {
                    handlePlayerCollision();
                    bulletHit = true;
                }
            } else {
                // Check boss collision first
                if (boss != null && boss.isAlive() && bullet.collidesWith(boss)) {
                    handleBossHit();
                    bullet.setActive(false);
                    gameStage.removeBullet(bullet);
                    bulletIter.remove();
                    bulletHit = true;
                }

                // Check enemy collisions if bullet hasn't hit boss
                if (!bulletHit) {
                    for (Enemy enemy : enemies) {
                        if (enemy.isAlive() && !enemy.isExploding() && bullet.collidesWith(enemy)) {
                            handleEnemyHit(enemy);
                            bullet.setActive(false);
                            gameStage.removeBullet(bullet);
                            bulletIter.remove();
                            bulletHit = true;
                            break;
                        }
                    }
                }
            }

            // Check asteroid collisions if bullet hasn't hit anything yet
            if (!bulletHit) {
                for (Character obj : gameObjects) {
                    if (obj instanceof Asteroid && obj.isAlive() && bullet.collidesWith(obj)) {
                        handleAsteroidHit((Asteroid)obj);
                        bullet.setActive(false);
                        gameStage.removeBullet(bullet);
                        bulletIter.remove();
                        break;
                    }
                }
            }
        }

        // Check player collisions with enemies
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && !enemy.isExploding() && player.collidesWith(enemy)) {
                handlePlayerCollision();
                break;
            }
        }

        // Check player collision with boss
        if (boss != null && boss.isAlive() && player.collidesWith(boss)) {
            handlePlayerCollision();
        }

        // Check player collisions with asteroids
        for (Character obj : gameObjects) {
            if (obj instanceof Asteroid && obj.isAlive() && player.collidesWith(obj)) {
                handlePlayerCollision();
                break;
            }
        }

        // Remove inactive bullets
        bullets.removeIf(bullet -> !bullet.isAlive());
    }


    private void handleAsteroidHit(Asteroid asteroid) {
        asteroid.hit();
        gameStage.showExplosion(asteroid.getPosition());

        if (!asteroid.isAlive()) {
            // สร้าง fragments เมื่ออุกาบาตถูกทำลาย
            for (Asteroid fragment : asteroid.split()) {
                addGameObject(fragment);
            }
            increaseScore(100); // คะแนนคงที่สำหรับการทำลายอุกาบาต
        }
    }

    private void handleEnemyHit(Enemy enemy) {
        enemy.hit();
        gameStage.showExplosion(enemy.getPosition());

        if (!enemy.isAlive()) {
            // ใช้ remove ที่ปลอดภัยกว่าด้วย CopyOnWriteArrayList
            enemies.remove(enemy);
            int points = enemy.isSecondTier() ? POINTS_SECOND_TIER_ENEMY : POINTS_REGULAR_ENEMY;
            increaseScore(points);
            gameStage.showScorePopup(points, enemy.getPosition());

            // เพิ่ม log เพื่อตรวจสอบ
            logger.info("Enemy destroyed! Points awarded: {}, Remaining enemies: {}",
                    points, enemies.size());


        }
    }

    private void spawnNewEnemies() {
        // Only spawn new enemies in waves 2-4
        if (currentWave >= 2 && currentWave <= 4) {
            if (enemies.size() < MAX_ENEMIES && random.nextDouble() < ENEMY_SPAWN_CHANCE) {
                spawnEnemy();
                logger.debug("Spawned new enemy. Total enemies: {}", enemies.size());
            }
        }
    }


    private void spawnEnemy() {
        Point2D spawnPos = getRandomSpawnPosition();
        boolean isSecondTier = false;

        // Wave-specific enemy spawning logic
        if (currentWave == 2) {
            isSecondTier = false; // Only regular enemies
        } else if (currentWave == 3) {
            isSecondTier = true;  // Only second-tier enemies
        } else if (currentWave >= 4) {
            isSecondTier = random.nextBoolean(); // Mix of both types
        }

        Enemy enemy = new Enemy(spawnPos, isSecondTier);
        enemies.add(enemy);
        gameStage.addGameObject(enemy);
        logger.debug("Spawned {} enemy at position: {}, Wave: {}",
                isSecondTier ? "second-tier" : "regular", spawnPos, currentWave);
    }

    private void spawnInitialEnemies() {
        // Only spawn enemies if we're in wave 2 or higher
        if (currentWave >= 2) {
            for (int i = 0; i < INITIAL_ENEMIES; i++) {
                spawnEnemy();
            }
            logger.info("Spawned {} initial enemies for wave {}", INITIAL_ENEMIES, currentWave);
        }
    }

    private void handleBossHit() {
        boss.hit(10);
        // Update health bar in GameStage
        gameStage.updateBossHealth(boss.getHealthPercentage(), boss.isEnraged());

        if (!boss.isAlive()) {
            gameStage.showExplosion(boss.getPosition());
            gameStage.hideBossHealth(); // Hide health bar when boss dies
            increaseScore(5000);
            startNextWave();
        }
    }

    private void handlePlayerCollision() {
        player.hit();
        gameStage.updateLives(player.getLives());
        gameStage.showExplosion(player.getPosition());

        if (!player.isAlive()) {
            endGame();
        }
    }

    private void addGameObject(Character obj) {
        gameObjects.add(obj);
        gameStage.addGameObject(obj);
    }

    private void wrapAround(Character character) {
        Point2D pos = character.getPosition();
        double x = pos.getX();
        double y = pos.getY();
        boolean wrapped = false;

        if (x < -50) { x = DEFAULT_WIDTH + 50; wrapped = true; }
        if (x > DEFAULT_WIDTH + 50) { x = -50; wrapped = true; }
        if (y < -50) { y = DEFAULT_HEIGHT + 50; wrapped = true; }
        if (y > DEFAULT_HEIGHT + 50) { y = -50; wrapped = true; }

        if (wrapped) {
            character.setPosition(new Point2D(x, y));
        }
    }

    private boolean isOffScreen(Point2D position) {
        return position.getX() < -100 || position.getX() > DEFAULT_WIDTH + 100 ||
                position.getY() < -100 || position.getY() > DEFAULT_HEIGHT + 100;
    }

    private Point2D getRandomSpawnPosition() {
        double x, y;
        double minDistanceFromPlayer = 150.0;
        do {
            // Spawn from edges of screen
            if (random.nextBoolean()) {
                // Spawn from top or bottom
                x = random.nextDouble() * DEFAULT_WIDTH;
                y = random.nextBoolean() ? -50 : DEFAULT_HEIGHT + 50;
            } else {
                // Spawn from left or right
                x = random.nextBoolean() ? -50 : DEFAULT_WIDTH + 50;
                y = random.nextDouble() * DEFAULT_HEIGHT;
            }
        } while (player != null &&
                new Point2D(x, y).distance(player.getPosition()) < minDistanceFromPlayer);

        return new Point2D(x, y);
    }

    private void checkWaveCompletion() {
        boolean allEnemiesDestroyed = enemies.isEmpty();
        boolean allAsteroidsDestroyed = gameObjects.stream()
                .filter(obj -> obj instanceof Asteroid)
                .noneMatch(Character::isAlive);
        boolean bossDestroyed = (currentWave == 5 && (boss == null || !boss.isAlive()));

        switch (currentWave) {
            case 1:
                // Wave 1: Players only deal with asteroids
                if (allAsteroidsDestroyed) {
                    logger.info("Wave 1 completed - Moving to Wave 2");
                    startNextWave();
                }
                break;

            case 2:
                // Wave 2: Regular enemies must all be defeated
                if (allEnemiesDestroyed||allAsteroidsDestroyed) {
                    logger.info("Wave 2 completed - Moving to Wave 3");
                    startNextWave();
                }
                break;

            case 3:
                // Wave 3: Second-tier enemies must all be defeated
                if (allEnemiesDestroyed||allAsteroidsDestroyed) {
                    logger.info("Wave 3 completed - Moving to Wave 4");
                    startNextWave();
                }
                break;

            case 4:
                // Wave 4: Mixed enemies must all be defeated
                if (allEnemiesDestroyed||allAsteroidsDestroyed) {
                    logger.info("Wave 4 completed - Moving to Wave 5");
                    startNextWave();
                }
                break;

            case 5:
                // Wave 5: Boss battle
                if (bossDestroyed) {
                    logger.info("Wave 5 completed - Game Over");
                    startNextWave(); // จะไปเรียก endGame()
                }
                break;
        }
    }




    private void startNextWave() {
        currentWave++;
        logger.info("Starting Wave {}", currentWave);
        gameStage.updateWave(currentWave);

        // เคลียร์ enemies และ asteroids จากรอบก่อนหน้า
        enemies.clear();
        gameObjects.removeIf(obj -> obj instanceof Asteroid);

        if (currentWave == 5) {
            spawnBoss();
        } else if (currentWave > 5) {
            endGame();
        } else {
            spawnAsteroids();
            // Ensure enemies are spawned immediately for waves 2-4
            if (currentWave >= 2) {
                spawnInitialEnemies();
                logger.info("Initial enemies spawned for wave {}: {}",
                        currentWave, enemies.size());
            }
        }
    }

    // เพิ่มเมธอดใหม่ถ้าต้องการให้เล่นต่อหลัง wave 5
    private void resetWaves() {
        currentWave = 1;
        boss = null;
        enemies.clear();
        gameObjects.clear();
        spawnAsteroids();
        gameStage.updateWave(currentWave);
    }

    private void spawnBoss() {
        Point2D spawnPos = new Point2D(DEFAULT_WIDTH/2, -50);
        boss = new Boss(spawnPos, currentWave);
        gameStage.addGameObject(boss);
        // Initialize boss health bar
        gameStage.updateBossHealth(1.0, false); // Start with full health
    }

    private void spawnAsteroid() {
        double x = Math.random() * DEFAULT_WIDTH;
        double y = Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50;
        Point2D spawnPos = new Point2D(x, y);

        // Create an asteroid
        Asteroid asteroid = new Asteroid(spawnPos, Asteroid.Type.ASTEROID);
        addGameObject(asteroid);
    }


    private void spawnAsteroids() {
        if (currentWave == 1) {
            // Wave 1: 2 ASTEROID และ 2 METEOR แน่นอน
            for (int i = 0; i < 2; i++) {
                // สร้าง ASTEROID
                Point2D asteroidPos = new Point2D(
                        Math.random() * DEFAULT_WIDTH,
                        Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50
                );
                Asteroid asteroid = new Asteroid(asteroidPos, Asteroid.Type.ASTEROID);
                addGameObject(asteroid);

                // สร้าง METEOR
                Point2D meteorPos = new Point2D(
                        Math.random() * DEFAULT_WIDTH,
                        Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50
                );
                Asteroid meteor = new Asteroid(meteorPos, Asteroid.Type.METEOR);
                addGameObject(meteor);
            }
            logger.info("Wave 1: Spawned 2 asteroids and 2 meteors");
        } else {
            // Wave 2-5: สุ่มเกิด 5 อุกาบาตจาก ASTEROID และ METEOR
            for (int i = 0; i < 5; i++) {
                Point2D spawnPos = new Point2D(
                        Math.random() * DEFAULT_WIDTH,
                        Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50
                );

                // สุ่มว่าจะเป็น ASTEROID หรือ METEOR (50-50)
                if (Math.random() < 0.5) {
                    Asteroid asteroid = new Asteroid(spawnPos, Asteroid.Type.ASTEROID);
                    addGameObject(asteroid);
                } else {
                    Asteroid meteor = new Asteroid(spawnPos, Asteroid.Type.METEOR);
                    addGameObject(meteor);
                }
            }
            logger.info("Wave {}: Spawned 5 random objects", currentWave);
        }
    }

    private void increaseScore(int points) {
        currentScore += points;
        gameStage.updateScore(currentScore);
    }

    // Public control methods
    public void startGame() {
        if (!isGameStarted) {
            isGameStarted = true;
            gameStage.hideStartMenu();

            // Initialize player
            player = new PlayerShip(new Point2D(DEFAULT_WIDTH/2, DEFAULT_HEIGHT/2));
            gameStage.addGameObject(player);

            // Spawn initial objects
            spawnAsteroids();
            spawnInitialEnemies();

            // Reset score and wave
            currentScore = 0;
            currentWave = 1;
            gameStage.updateScore(currentScore);
            gameStage.updateWave(currentWave);

            logger.info("Game started");
        }
    }

    public void pauseGame() {
        if (isGameStarted && !isPaused) {
            isPaused = true;
            gameStage.showPauseMenu();
        }
    }

    public void resumeGame() {
        if (isPaused) {
            isPaused = false;
            gameStage.hidePauseMenu();
        }
    }


    public void resetGame() {
        // Existing clear code...
        gameObjects.clear();
        bullets.clear();
        enemies.clear();
        if (boss != null) {
            gameStage.hideBossHealth(); // Hide health bar when resetting game
            boss = null;
        }
        player = null;

        // Rest of existing reset code...
        isGameStarted = false;
        isPaused = false;
        currentWave = 1;
        currentScore = 0;
        gameStage.reset();
        logger.info("Game reset");
    }

    private void endGame() {
        isGameStarted = false;
        gameStage.showGameOver(currentScore);
    }

    // Movement controls
    public void shoot() {
        if (isGameStarted && !isPaused && player != null && player.isAlive()) {
            long currentTime = System.nanoTime();
            if (currentTime - lastBulletTime >= BULLET_COOLDOWN) {
                Bullet bullet = player.shoot();
                if (bullet != null) {
                    bullets.add(bullet);
                    gameStage.addBullet(bullet);
                    lastBulletTime = currentTime;
                }
            }
        }
    }

    public void moveLeft() { if (player != null) player.moveLeft(); }
    public void moveRight() { if (player != null) player.moveRight(); }
    public void moveUp() { if (player != null) player.moveUp(); }
    public void moveDown() { if (player != null) player.moveDown(); }
    public void rotateLeft() { if (player != null) player.rotateLeft(); }
    public void rotateRight() { if (player != null) player.rotateRight(); }
    public void stopThrust() { if (player != null) player.stopThrust(); }

    // Window resize handling
    public void handleResize(double width, double height) {
        currentWidth = width;
        currentHeight = height;
        gameStage.handleResize(width, height);
    }

    // Getters
    public boolean isGameStarted() { return isGameStarted; }
    public boolean isPaused() { return isPaused; }
    public int getCurrentWave() { return currentWave; }
    public int getScore() { return currentScore; }
    public Button getStartButton() { return gameStage.getStartButton(); }
    public Button getRestartButton() { return gameStage.getRestartButton(); }
    public Button getResumeButton() { return gameStage.getResumeButton(); }
}