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
            // ลบ enemy ออกจากทั้ง List และ GameStage
            enemies.remove(enemy);
            gameStage.removeGameObject(enemy);  // เพิ่มบรรทัดนี้

            // Award points
            int points = enemy.isSecondTier() ? POINTS_SECOND_TIER_ENEMY : POINTS_REGULAR_ENEMY;
            increaseScore(points);
            gameStage.showScorePopup(points, enemy.getPosition());

            // ตรวจสอบจำนวน enemy ที่เหลือ
            long remainingEnemies = enemies.stream()
                    .filter(Character::isAlive)
                    .count();

            logger.info("Enemy destroyed! Points: {}, Enemies remaining: {}",
                    points, remainingEnemies);

            // ตรวจสอบการจบ wave ทันทีหลังจากทำลาย enemy
            if (remainingEnemies == 0) {
                logger.info("All enemies destroyed, checking wave completion");
                checkWaveCompletion();
            }
        }
    }

    private void spawnNewEnemies() {
        // Only spawn new enemies in waves 2-4
        if (currentWave >= 2 && currentWave <= 4) {
            // Count only alive enemies for spawn check
            long aliveEnemies = enemies.stream()
                    .filter(Character::isAlive)
                    .count();

            // Check total enemies spawned this wave (both alive and dead)
            long totalEnemiesThisWave = enemies.size();

            // Only spawn if we haven't reached the maximum number of enemies for this wave
            if (aliveEnemies < MAX_ENEMIES && totalEnemiesThisWave < MAX_ENEMIES &&
                    random.nextDouble() < ENEMY_SPAWN_CHANCE) {
                spawnEnemy();
                logger.debug("Spawned new enemy. Total enemies this wave: {}", totalEnemiesThisWave + 1);
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
            // Spawn initial enemies, but not more than MAX_ENEMIES
            int enemiesToSpawn = Math.min(INITIAL_ENEMIES, MAX_ENEMIES);
            for (int i = 0; i < enemiesToSpawn; i++) {
                spawnEnemy();
            }
            logger.info("Spawned {} initial enemies for wave {}", enemiesToSpawn, currentWave);
        }
    }

    private void handleBossHit() {
        boss.hit(10);
        if (!boss.isAlive()) {
            gameStage.showExplosion(boss.getPosition());
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
                if (allAsteroidsDestroyed) {
                    logger.info("Wave 1 completed - All asteroids destroyed");
                    startNextWave();
                }
                break;

            case 2:
            case 3:
            case 4:
                if (allEnemiesDestroyed || allAsteroidsDestroyed) {
                    logger.info("Wave {} completion criteria met:", currentWave);
                    logger.info("- All enemies destroyed: {}", allEnemiesDestroyed);
                    logger.info("- All asteroids destroyed: {}", allAsteroidsDestroyed);
                    startNextWave();
                }
                break;

            case 5:
                if (bossDestroyed) {
                    logger.info("Wave 5 completed - Boss destroyed");
                    startNextWave();
                }
                break;
        }
    }

//    private void checkWaveCompletion() {
//        boolean allEnemiesDestroyed = enemies.isEmpty();
//        boolean allAsteroidsDestroyed = gameObjects.stream()
//                .filter(obj -> obj instanceof Asteroid)
//                .noneMatch(Character::isAlive);
//        boolean bossDestroyed = (currentWave == 5 && (boss == null || !boss.isAlive()));
//
//        // แก้ไขเงื่อนไขให้จบ wave ได้ง่ายขึ้น
//        boolean regularWaveCleared = (currentWave < 5 &&
//                (allEnemiesDestroyed || allAsteroidsDestroyed));
//
//        if (bossDestroyed || regularWaveCleared) {
//            startNextWave();
//        }
//    }




    private void startNextWave() {
        currentWave++;
        logger.info("=== Starting Wave {} ===", currentWave);

        // Clear objects from previous wave
        enemies.clear();
        gameObjects.removeIf(obj -> obj instanceof Asteroid);
        //gameStage.clearAsteroids();

        // Update UI
        gameStage.updateWave(currentWave);

        if (currentWave == 5) {
            spawnBoss();
        } else if (currentWave > 5) {
            endGame();
        } else {
            spawnAsteroids();
            if (currentWave >= 2) {
                spawnInitialEnemies();
                logger.info("Initial setup for wave {}:", currentWave);
                logger.info("- Spawned {} initial enemies", Math.min(INITIAL_ENEMIES, MAX_ENEMIES));
                logger.info("- Spawned asteroids");
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

    }

    private void spawnAsteroid() {
        double x = Math.random() * DEFAULT_WIDTH;
        double y = Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50;
        Point2D spawnPos = new Point2D(x, y);

        // สร้างอุกาบาตขนาดเดียว
        Asteroid asteroid = new Asteroid(spawnPos);
        addGameObject(asteroid);
    }


    private void spawnAsteroids() {
        // ปรับจำนวนอุกาบาตตาม wave
        int baseCount = 4;
        int additionalCount = (currentWave - 1) * 2;
        int totalCount = Math.min(baseCount + additionalCount, 12);

        for (int i = 0; i < totalCount; i++) {
            spawnAsteroid();
        }
        logger.info("Spawned {} asteroids for wave {}", totalCount, currentWave);
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
        // Clear all game objects
        gameObjects.clear();
        bullets.clear();
        enemies.clear();
        boss = null;
        player = null;

        // Reset game state
        isGameStarted = false;
        isPaused = false;
        currentWave = 1;
        currentScore = 0;

        // Reset UI
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