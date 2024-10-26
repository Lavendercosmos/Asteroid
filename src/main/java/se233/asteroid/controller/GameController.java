package se233.asteroid.controller;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import se233.asteroid.model.PlayerShip;
import se233.asteroid.model.Bullet;
import se233.asteroid.model.Asteroid;
import se233.asteroid.model.Boss;
import se233.asteroid.view.GameStage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameController {
    private static final Logger logger = LogManager.getLogger(GameController.class);
    private static final double GAME_WIDTH = 800.0;
    private static final double GAME_HEIGHT = 600.0;
    private static final long BULLET_COOLDOWN = 250_000_000; // 250ms in nanoseconds

    private final Scene scene;
    private final GameStage gameStage;
    private final Set<KeyCode> activeKeys;
    private final Random random;

    private PlayerShip player;
    private List<Asteroid> asteroids;  // Keep this as List<Asteroid> for type safety
    private List<Bullet> bullets;
    private Boss boss;  // Keep this as Boss for type safety
    private int currentWave = 1;

    private int score;
    private boolean gameOver;
    private boolean isGameStarted;
    private long lastUpdateTime;
    private long lastBulletTime;
    private AnimationTimer gameLoop;
    private boolean isGamePaused;

    public GameController(Scene scene, GameStage gameStage) {
        this.scene = scene;
        this.gameStage = gameStage;
        this.asteroids = new CopyOnWriteArrayList<>();
        this.bullets = new CopyOnWriteArrayList<>();
        this.activeKeys = new HashSet<>();
        this.random = new Random();
        this.lastBulletTime = 0;
        this.isGameStarted = false;
        this.isGamePaused = false;
        this.gameOver = false;

        gameStage.getStartButton().setOnAction(e -> {
            isGameStarted = true;
            gameStage.hideStartMenu();
            setupGame();
            logger.info("Game started via start button");
        });

        // เพิ่ม Event Handler สำหรับปุ่ม Restart
        gameStage.getRestartButton().setOnAction(e -> {
            gameOver = false;
            gameStage.hideGameOver();
            setupGame();
            logger.info("Game restarted via restart button");
        });

        setupInputHandlers();
        startGameLoop();
    }



    private void checkCollisions() {
        // Check bullet collisions
        for (Bullet bullet : bullets) {
            // Check asteroid collisions
            for (Asteroid asteroid : asteroids) {
                if (bullet.collidesWith(asteroid)) {
                    handleAsteroidHit(asteroid, bullet);
                }
            }

            // Check boss collision
            if (boss != null && bullet.collidesWith(boss)) {
                handleBossHit(bullet);
            }
        }

        // Check player collisions with asteroids
        for (Asteroid asteroid : asteroids) {
            if (player.collidesWith(asteroid)) {
                handlePlayerHit();
            }
        }

        // Check player collision with boss
        if (boss != null && player.collidesWith(boss)) {
            handlePlayerHit();
        }
    }

    private void setupInputHandlers() {
        scene.setOnKeyPressed(e -> {
            activeKeys.add(e.getCode());

            if (e.getCode() == KeyCode.SPACE) {
                handleSpacePress();
            } else if (e.getCode() == KeyCode.ESCAPE && isGameStarted && !gameOver) {
                togglePause();
            }
        });

        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }

    private void handleSpacePress() {
        if (!isGameStarted) {
            // Start new game
            isGameStarted = true;
            gameStage.hideStartMenu();
            setupGame();
            logger.info("Game started");
        } else if (gameOver) {
            // Restart game
            gameOver = false;
            setupGame();
            logger.info("Game restarted");
        }
    }


    private void togglePause() {
        if (gameLoop != null) {
            if (!isGamePaused) {
                gameLoop.stop();
                isGamePaused = true;
                gameStage.showPauseMenu();
                logger.info("Game paused");
            } else {
                gameLoop.start();
                isGamePaused = false;
                gameStage.hidePauseMenu();
                logger.info("Game resumed");
            }
        }
    }



    private void setupGame() {
        clearAll();
        // Reset game state
        asteroids.clear();
        bullets.clear();
        boss = null;
        score = 0;
        currentWave = 1;  // Reset wave number
        gameOver = false;

        // Initialize player at center of screen
        player = new PlayerShip(new Point2D(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        gameStage.addGameObject(player);
        gameStage.updateScore(score);
        gameStage.updateLives(player.getLives());
        gameStage.updateWave(currentWave);  // Update wave display
        gameStage.hideGameOver();

        // Spawn initial asteroids
        spawnAsteroids(5);
        if (gameLoop != null) {
            gameLoop.start();
        }

        logger.info("Game setup completed for wave {}", currentWave);
    }

    private void startGameLoop() {
        lastUpdateTime = System.nanoTime();

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isGameStarted || isGamePaused) {
                    return;  // Don't update if game hasn't started or is paused
                }

                double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                lastUpdateTime = now;

                if (isGameStarted && !gameOver) {
                    handleInput(now);
                    updateGame(deltaTime);
                    checkCollisions();
                    spawnNewAsteroids();
                    cleanupObjects();
                }
            }
        };

        gameLoop.start();
        logger.info("Game loop started");
    }


    private void handleInput(long currentTime) {
        if (!isGameStarted || gameOver) return;  // Don't process game input if not started or game is over

        if (activeKeys.contains(KeyCode.A)) player.moveLeft();
        if (activeKeys.contains(KeyCode.D)) player.moveRight();
        if (activeKeys.contains(KeyCode.W)) player.moveUp();
        if (activeKeys.contains(KeyCode.S)) player.moveDown();
        if (activeKeys.contains(KeyCode.Q)) player.rotateLeft();
        if (activeKeys.contains(KeyCode.E)) player.rotateRight();

        // Handle shooting with cooldown
        if (activeKeys.contains(KeyCode.SPACE) &&
                (currentTime - lastBulletTime) >= BULLET_COOLDOWN) {
            Bullet bullet = player.shoot();
            bullets.add(bullet);
            gameStage.addBullet(bullet);
            lastBulletTime = currentTime;
        }
    }

    private void updateGame(double deltaTime) {
        // Update player
        player.update();
        wrapPosition(player);

        // Update bullets
        for (Bullet bullet : bullets) {
            bullet.update();
        }

        // Update asteroids
        for (Asteroid asteroid : asteroids) {
            asteroid.update();
            wrapPosition(asteroid);
        }

        // Update boss if present
        if (boss != null) {
            boss.update(deltaTime, player.getPosition());
            wrapPosition(boss);

            // Boss special attack with random chance
            if (random.nextDouble() < 0.01) { // 1% chance per frame
                Bullet[] bossAttack = boss.shootSpecialAttack();
                for (Bullet bullet : bossAttack) {
                    bullets.add(bullet);
                    gameStage.addBullet(bullet);
                }
            }
        }

        // Spawn boss when all asteroids are destroyed
        if (asteroids.isEmpty() && boss == null) {
            spawnBoss();
        }
    }
    private void wrapPosition(se233.asteroid.model.Character character) {
        Point2D position = character.getPosition();
        double x = position.getX();
        double y = position.getY();

        if (x < 0) x = GAME_WIDTH;
        if (x > GAME_WIDTH) x = 0;
        if (y < 0) y = GAME_HEIGHT;
        if (y > GAME_HEIGHT) y = 0;

        character.setPosition(new Point2D(x, y));
    }

    private void cleanupObjects() {
        // Remove bullets that are out of bounds
        bullets.removeIf(bullet -> {
            if (!isInBounds(bullet.getPosition())) {
                gameStage.removeBullet(bullet);
                return true;
            }
            return false;
        });
    }

    private void handlePlayerHit() {
        player.hit();
        gameStage.updateLives(player.getLives());
        gameStage.showExplosion(player.getPosition());

        if (!player.isAlive()) {
            gameOver = true;
            gameStage.showGameOver(score);
            logger.info("Game Over! Final score: {}", score);
        }
    }

    private void handleAsteroidHit(Asteroid asteroid, Bullet bullet) {
        score += asteroid.getPoints();
        gameStage.updateScore(score);

        // Remove asteroid and bullet
        asteroids.remove(asteroid);
        bullets.remove(bullet);
        gameStage.removeGameObject(asteroid);
        gameStage.removeBullet(bullet);

        gameStage.showExplosion(asteroid.getPosition());
        logger.info("Asteroid destroyed! Score: {}", score);
    }

    private void handleBossHit(Bullet bullet) {
        boss.hit(10);
        bullets.remove(bullet);
        gameStage.removeBullet(bullet);
        gameStage.showExplosion(bullet.getPosition());

        if (!boss.isAlive()) {
            score += boss.getPoints();
            gameStage.updateScore(score);
            gameStage.removeGameObject(boss);

            // Increment wave and either start next wave or show victory
            currentWave++;
            if (currentWave <= 3) {  // For example, limit to 3 waves
                boss = null;
                gameStage.updateWave(currentWave);
                spawnAsteroids(5 + currentWave);  // Increase difficulty with each wave
                logger.info("Wave {} completed, starting next wave", currentWave - 1);
            } else {
                gameStage.showVictory(score);
                gameOver = true;
                logger.info("Game completed! Final score: {}", score);
            }
        }
    }

    private void spawnAsteroids(int count) {
        for (int i = 0; i < count; i++) {
            Point2D position = getRandomSpawnPosition();
            Asteroid asteroid = new Asteroid(position, 1);
            asteroids.add(asteroid);
            gameStage.addGameObject(asteroid);
        }
    }

    // Then modify the spawnBoss method
    private void spawnBoss() {
        Point2D position = new Point2D(GAME_WIDTH / 2, 100);
        boss = new Boss(position, currentWave);
        gameStage.addGameObject(boss);
        logger.info("Boss spawned for wave {}!", currentWave);
    }

    private void spawnNewAsteroids() {
        if (asteroids.size() < 5 && boss == null && random.nextDouble() < 0.02) {
            spawnAsteroids(1);
        }
    }

    private Point2D getRandomSpawnPosition() {
        // Spawn objects away from the player
        double x, y;
        do {
            x = random.nextDouble() * GAME_WIDTH;
            y = random.nextDouble() * GAME_HEIGHT;
        } while (new Point2D(x, y).distance(player.getPosition()) < 100);
        // or alternatively:
        // } while (player.getPosition().distance(x, y) < 100);

        return new Point2D(x, y);
    }
    private boolean isInBounds(Point2D position) {
        return position.getX() >= 0 && position.getX() <= GAME_WIDTH &&
                position.getY() >= 0 && position.getY() <= GAME_HEIGHT;
    }
    public void clearAll() {
        // Remove all existing game objects from the view
        if (player != null) {
            gameStage.removeGameObject(player);
        }
        for (Asteroid asteroid : asteroids) {
            gameStage.removeGameObject(asteroid);
        }
        for (Bullet bullet : bullets) {
            gameStage.removeBullet(bullet);
        }
        if (boss != null) {
            gameStage.removeGameObject(boss);
        }
    }
}