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
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameView extends Pane {
    private static final Logger logger = LogManager.getLogger(GameView.class);

    // Game constants
    public static final double DEFAULT_WIDTH = 800;//
    public static final double DEFAULT_HEIGHT = 600;
    private static final double BORDER_MARGIN = 100;
    private static final long BULLET_COOLDOWN = 250_000_000L; // 250ms
    private static final int INITIAL_ENEMIES = 2;
    private static final double ENEMY_SPAWN_CHANCE = 0.01; // 1% chance per frame
    private static final int MAX_ENEMIES = 3;
    private static final int POINTS_REGULAR_ENEMY = 100;
    private static final int POINTS_SECOND_TIER_ENEMY = 250;

    // Add to GameView.java class constants
    private static final double BOSS_SPAWN_INTERVAL = 5.0; // Spawn check every 5 seconds
    private static final int MAX_BOSS_SPAWNED_ENEMIES = 6; // Maximum enemies spawned by boss
    private double bossSpawnTimer = 0;

    // Game components
    private final GameStage gameStage;
    private final List<Character> gameObjects;
    private final List<Bullet> bullets;
    private final List<Enemy> enemies;
    private final List<EnemyBullet> enemybullets;
    private PlayerShip player;
    private Boss boss;

    // Game state
    private boolean isGameStarted;
    private boolean isPaused;
    private double currentWidth;
    private double currentHeight;
    private int currentWave;
    private long lastBulletTime;
    private long lastUpdateTime;
    private final Random random;

    public GameView() {
        // Initialize collections
        this.gameObjects = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.enemybullets = new ArrayList<>();
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
        this.lastUpdateTime = System.nanoTime();

        setupButtonHandlers();
        setupGameLoop();

        logger.info("GameView initialized");
    }

    private void setupButtonHandlers() {
        gameStage.getStartButton().setOnAction(e -> startGame());
        gameStage.getRestartButton().setOnAction(e -> resetGame());
        gameStage.getResumeButton().setOnAction(e -> resumeGame());
        gameStage.getTryAgainButton().setOnAction(e -> resetGame());
    }

    private void setupGameLoop() {
        lastUpdateTime = System.nanoTime();  // Initialize lastUpdateTime
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPaused && isGameStarted) {
                    // Calculate deltaTime in seconds
                    double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                    lastUpdateTime = now;

                    updateGame(deltaTime);
                    checkCollisions();
                    checkWaveCompletion();
                    spawnNewEnemies();
                }
            }
        };
        gameLoop.start();
    }

    private void updateGame(double deltaTime) {
        // Update player
        if (player != null && player.isAlive()) {
            player.update();
            wrapAround(player);
        }

        // Update boss
        if (boss != null && boss.isAlive()) {
            boss.update(deltaTime, player.getPosition());
            wrapAround(boss);

            // Update boss spawn timer
            bossSpawnTimer += deltaTime;

            // Regular spawning check
            if (bossSpawnTimer >= BOSS_SPAWN_INTERVAL) {
                bossSpawnTimer = 0;

                // Only spawn if we haven't reached the maximum
                if (enemies.size() < MAX_BOSS_SPAWNED_ENEMIES) {
                    // Get spawned enemies from boss
                    List<Enemy> newEnemies = boss.collectSpawnedEnemies();

                    // Add new enemies to game
                    for (Enemy enemy : newEnemies) {
                        if (enemy != null) {
                            enemies.add(enemy);
                            gameStage.addGameObject(enemy);
                            logger.info("Boss spawned new enemy. Total enemies: {}", enemies.size());
                        }
                    }
                }
            }

            // Emergency spawning when boss health is low
            if (boss.getHealthPercentage() <= 0.5 && enemies.size() < MAX_BOSS_SPAWNED_ENEMIES / 2) {
                Enemy enemySpawned = boss.spawnSingleEnemy();
                if (enemySpawned != null) {
                    enemies.add(enemySpawned);
                    gameStage.addGameObject(enemySpawned);
                    logger.info("Boss spawned emergency enemy at low health. Total enemies: {}", enemies.size());
                }
            }
        }

        // Update enemies
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.update();

                // ส่งตำแหน่งผู้เล่นให้ AI
                if (player != null && player.isAlive()) {
                    enemy.updateAI(player.getPosition());

                    EnemyBullet enemyBullet = enemy.enemyshoot();
                    if (enemyBullet != null) {
                        enemybullets.add(enemyBullet);
                        gameStage.addEnemyBullet(enemyBullet);
                        logger.debug("Enemy fired bullet from position: {}", enemy.getPosition());
                    }

                }
                wrapAround(enemy);
            }
        }

        // Update enemy bullets
        Iterator<EnemyBullet> enemybullet = enemybullets.iterator();
        while (enemybullet.hasNext()) {
            EnemyBullet ebullet = enemybullet.next();
            if (ebullet.isActive()) {
                ebullet.update();

                // Remove bullets that are offscreen or expired
                if (isOffScreen(ebullet.getPosition()) || ebullet.isExpired()) {
                    gameStage.removeEnemyBullet(ebullet);
                    enemybullet.remove();
                    logger.debug("Removed bullet: {}", ebullet);
                }
            }else {
                gameStage.removeEnemyBullet(ebullet);
                enemybullet.remove();}
        }

        // Update bullets
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            if (bullet.isActive()) {
                bullet.update();

                // Remove bullets that are offscreen or expired
                if (isOffScreen(bullet.getPosition()) || bullet.isExpired()) {
                    gameStage.removeBullet(bullet);
                    bulletIter.remove();
                    logger.debug("Removed bullet: {}", bullet);
                }
            }else {
                gameStage.removeBullet(bullet);
                bulletIter.remove();}
        }



        // Update other game objects
        for (Character obj : gameObjects) {
            if (obj.isAlive()) {
                obj.update();
                wrapAround(obj);
            }
        }
    }

    private void checkCollisions() {
        if (player == null || !player.isAlive() || player.isInvulnerable()) return;

        // Check bullet collisions
        Iterator<Bullet> bulletIter = new CopyOnWriteArrayList<>(bullets).iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            boolean bulletHit = false;

            // Skip enemy bullets hitting enemies
            if (bullet.isEnemyBullet()) {
                // เช็คว่ากระสุนศัตรูชนผู้เล่นหรือไม่
                if (!player.isInvulnerable() && bullet.collidesWith(player)) {
                    handlePlayerCollision();
                    bulletHit = true;
                    bullets.remove(bullet);
                    // ลบกระสุนที่ชนแล้ว
                    gameStage.removeBullet(bullet);

                }
            } else {
                // Check boss collision first
                if (boss != null && boss.isAlive() && bullet.collidesWith(boss)) {
                    handleBossHit();
                    bulletHit = true;
                }

                // Check enemy collisions if bullet hasn't hit boss
                for (Enemy enemy : new ArrayList<>(enemies)) {  // Create a copy to avoid concurrent modification
                    if (enemy.isAlive() && !enemy.isExploding() && bullet.collidesWith(enemy)) {
                        logger.debug("Bullet hit enemy at position: {}", enemy.getPosition());
                        handleEnemyHit(enemy);
                        bullet.setActive(false);
                        bullets.remove(bullet);
                        gameStage.removeBullet(bullet);
                        bulletHit = true;

                        // Log score after enemy hit
                        logger.debug("Score after enemy hit processed: {}",
                                gameStage.getScoreSystem().getCurrentScore());
                        break;

                    }
                }
                if (bulletHit) {
                    bullets.remove(bullet);  // ใช้ bullets.remove แทน bulletIter.remove/
                    // / ลบกระสุนที่ชนแล้ว
                    gameStage.removeBullet(bullet);

                    continue;
                }
            }

            // Check asteroid collisions if bullet hasn't hit anything yet
            if (!bulletHit) {
                for (Character obj : new ArrayList<>(gameObjects)) {  // Create a copy to avoid concurrent modification
                    if (obj instanceof Asteroid && obj.isAlive() && !((Asteroid)obj).isExploding() && bullet.collidesWith(obj)) {
                        handleAsteroidHit((Asteroid)obj);
                        bulletHit = true;
                        bullets.remove(bullet);
                        gameStage.removeBullet(bullet);
                        logger.debug("Bullet hit asteroid of type: {}", ((Asteroid)obj).getType());
                        break;
                    }
                }
            }
        }

        for (EnemyBullet enemyBullet : enemybullets) {
            if (enemyBullet.isAlive()  && player.collidesWith(enemyBullet)) {
                handlePlayerCollision();
                break;
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
        if (!asteroid.isAlive()) {
            return; // Skip if asteroid is already destroyed
        }

        // Call hit() on the asteroid
        asteroid.hit();
        gameStage.showExplosion(asteroid.getPosition());

        // Add points based on asteroid type before creating fragments
        if (asteroid.getType() == Asteroid.Type.ASTEROID) {
            gameStage.getScoreSystem().addAsteroidPoints();
            logger.info("Added points for destroying ASTEROID");
        } else if (asteroid.getType() == Asteroid.Type.METEOR) {
            gameStage.getScoreSystem().addMeteorPoints();
            logger.info("Added points for destroying METEOR");
        }

        // Only create fragments and remove if asteroid is destroyed
        if (!asteroid.isAlive()) {
            // Create fragments if it's an asteroid
            if (asteroid.getType() == Asteroid.Type.ASTEROID) {
                for (Asteroid fragment : asteroid.split()) {
                    addGameObject(fragment);
                }
                logger.debug("Created fragments for destroyed asteroid");
            }

            // Remove the destroyed asteroid from game objects
            gameObjects.remove(asteroid);
            gameStage.removeGameObject(asteroid);
            logger.debug("Removed destroyed {} from game", asteroid.getType());
        }
    }

    private void handleEnemyHit(Enemy enemy) {
        if (!enemy.isAlive() || enemy.isExploding()) {
            return; // Skip if enemy is already dead or exploding
        }

        // Log current score before adding points
        logger.debug("Current score before enemy hit: {}", gameStage.getScoreSystem().getCurrentScore());

        // Call hit() on enemy and show explosion
        enemy.hit();
        gameStage.showExplosion(enemy.getPosition());

        // Add points immediately when hit, don't wait for explosion to finish
        if (enemy.isSecondTier()) {
            logger.info("Adding points for second tier enemy");
            gameStage.getScoreSystem().addSecondTierEnemyPoints();
        } else {
            logger.info("Adding points for regular enemy");
            gameStage.getScoreSystem().addRegularEnemyPoints();
        }

        // Log score after adding points
        logger.debug("Current score after enemy hit: {}", gameStage.getScoreSystem().getCurrentScore());

        // Remove enemy from game if dead
        if (!enemy.isAlive()) {
            enemies.remove(enemy);
            gameStage.removeGameObject(enemy);
            logger.info("Enemy destroyed! Type: {}, Remaining enemies: {}",
                    enemy.isSecondTier() ? "Second Tier" : "Regular",
                    enemies.size());
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
            gameStage.getScoreSystem().addBossPoints();
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

        switch (currentWave) {
            case 1:
                if (allAsteroidsDestroyed) {
                    logger.info("Wave 1 completed - Moving to Wave 2");
                    startNextWave();
                }
                break;

            case 2:
                if (allEnemiesDestroyed || allAsteroidsDestroyed) {
                    logger.info("Wave 2 completed - Moving to Wave 3");
                    startNextWave();
                }
                break;

            case 3:
                if (allEnemiesDestroyed || allAsteroidsDestroyed) {
                    logger.info("Wave 3 completed - Moving to Wave 4");
                    startNextWave();
                }
                break;

            case 4:
                if (allEnemiesDestroyed || allAsteroidsDestroyed) {
                    logger.info("Wave 4 completed - Moving to Wave 5");
                    startNextWave();
                }
                break;

            case 5:
                // Wave 5: Boss battle
                boolean isBossDefeated = (boss == null || !boss.isAlive());
                boolean areAllEnemiesDefeated = enemies.isEmpty();

                if (isBossDefeated && areAllEnemiesDefeated) {
                    logger.info("Wave 5 completed - All enemies and boss defeated");
                    startNextWave();
                }
                break;
        }
    }




    private void startNextWave() {
        currentWave++;
        logger.info("Starting Wave {}", currentWave);
        gameStage.updateWave(currentWave);

        // Clear enemies
        for (Enemy enemy : new ArrayList<>(enemies)) {
            gameStage.removeGameObject(enemy);
        }
        enemies.clear();
        // Clear asteroids
        List<Character> asteroidsToRemove = gameObjects.stream()
                .filter(obj -> obj instanceof Asteroid)
                .collect(Collectors.toList());

        for (Character asteroid : asteroidsToRemove) {
            gameStage.removeGameObject(asteroid);
            gameObjects.remove(asteroid);
        }

        // Clear bullets
        for (Bullet bullet : new ArrayList<>(bullets)) {
            gameStage.removeBullet(bullet);
        }
        bullets.clear();

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

    private void spawnAsteroids() {
        if (currentWave == 1) {
            // Wave แรก: สร้าง ASTEROID 2 ก้อน และ METEOR 2 ก้อน
            for (int i = 0; i < 2; i++) {
                // สร้างตำแหน่งสำหรับ ASTEROID
                // สูตร: จุดเริ่มต้น(BORDER_MARGIN) + (สุ่มตำแหน่งในพื้นที่ที่เหลือ)
                Point2D asteroidPos = new Point2D(
                        // ตำแหน่ง X = ระยะห่างจากขอบ + (สุ่มค่าในช่วงความกว้างที่เหลือ)
                        BORDER_MARGIN + (Math.random() * (DEFAULT_WIDTH - 2 * BORDER_MARGIN)),
                        // ตำแหน่ง Y = ระยะห่างจากขอบ + (สุ่มค่าในช่วงความสูงที่เหลือ)
                        BORDER_MARGIN + (Math.random() * (DEFAULT_HEIGHT - 2 * BORDER_MARGIN))
                );
                // สร้าง ASTEROID ในตำแหน่งที่กำหนด
                Asteroid asteroid = new Asteroid(asteroidPos, Asteroid.Type.ASTEROID);
                addGameObject(asteroid);

                // สร้างตำแหน่งสำหรับ METEOR (ใช้วิธีเดียวกัน)
                Point2D meteorPos = new Point2D(
                        BORDER_MARGIN + (Math.random() * (DEFAULT_WIDTH - 2 * BORDER_MARGIN)),
                        BORDER_MARGIN + (Math.random() * (DEFAULT_HEIGHT - 2 * BORDER_MARGIN))
                );
                // สร้าง METEOR ในตำแหน่งที่กำหนด
                Asteroid meteor = new Asteroid(meteorPos, Asteroid.Type.METEOR);
                addGameObject(meteor);
            }
            logger.info("Wave 1: สร้าง asteroid 2 ก้อน และ meteor 2 ก้อน ห่างจากขอบจอ");
        } else {
            // Wave 2-5: สร้างสิ่งกีดขวางแบบสุ่ม 5 ชิ้น
            for (int i = 0; i < 5; i++) {
                // สร้างตำแหน่งที่ห่างจากขอบ
                Point2D spawnPos = new Point2D(
                        BORDER_MARGIN + (Math.random() * (DEFAULT_WIDTH - 2 * BORDER_MARGIN)),
                        BORDER_MARGIN + (Math.random() * (DEFAULT_HEIGHT - 2 * BORDER_MARGIN))
                );

                // สุ่มประเภทของสิ่งกีดขวาง (50% ASTEROID, 50% METEOR)
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
    // ตรงนี้
    // Public control methods
    public void startGame() {
        if (!isGameStarted) {
            isGameStarted = true;
            gameStage.hideStartMenu();

            // Initialize player
            player = new PlayerShip(new Point2D(DEFAULT_WIDTH/2, DEFAULT_HEIGHT/2));
            gameStage.addGameObject(player);
            // เพิ่ม sprite ของไอพ่นเข้าไปใน gameLayer
            gameStage.getGameLayer().getChildren().add(player.getThrusterSprite());
            gameStage.getGameLayer().getChildren().add(player.getShieldSprite());
            // Spawn initial objects
            spawnAsteroids();
            spawnInitialEnemies();

            // Reset score and wave
            currentWave = 1; //เปลี่ยนด่านเริ่มต้น
            gameStage.getScoreSystem().reset();
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
        enemybullets.clear();
        if (boss != null) {
            gameStage.hideBossHealth(); // Hide health bar when resetting game
            boss = null;
        }
        player = null;

        // Rest of existing reset code...
        isGameStarted = false;
        isPaused = false;
        currentWave = 1;
        gameStage.reset();
        logger.info("Game reset");
    }

    private void endGame() {
        isGameStarted = false;
        gameStage.showGameOver(gameStage.getScoreSystem().getCurrentScore());
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


    // Getters
    public boolean isGameStarted() { return isGameStarted; }
    public boolean isPaused() { return isPaused; }
    public Button getStartButton() { return gameStage.getStartButton(); }
    public Button getRestartButton() { return gameStage.getRestartButton(); }
    public Button getResumeButton() { return gameStage.getResumeButton(); }
}