package se233.asteroid.controller;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import se233.asteroid.model.PlayerShip;
import se233.asteroid.model.Bullet;
import se233.asteroid.model.Asteroid;
import se233.asteroid.model.Boss;
import se233.asteroid.View.GameView;
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
    private final GameView gameView;
    private final Set<KeyCode> activeKeys;
    private final Random random;

    private PlayerShip player;
    private List<Asteroid> asteroids;  // Keep this as List<Asteroid> for type safety
    private List<Bullet> bullets;
    private Boss boss;  // Keep this as Boss for type safety

    private int score;
    private boolean gameOver;
    private boolean isGameStarted;
    private long lastUpdateTime;
    private long lastBulletTime;
    private AnimationTimer gameLoop;
    private boolean isGamePaused;

    public GameController(Scene scene, GameView gameView) {
        this.scene = scene;
        this.gameView = gameView;
        this.asteroids = new CopyOnWriteArrayList<>();
        this.bullets = new CopyOnWriteArrayList<>();
        this.activeKeys = new HashSet<>();
        this.random = new Random();
        this.lastBulletTime = 0;
        this.isGameStarted = false;
        this.isGamePaused = false;
        this.gameOver = false;
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
                if (!isGameStarted) {
                    logger.info("Space pressed - Starting new game");
                    isGameStarted = true;
                    gameView.hideStartMenu();
                    setupGame();
                } else if (gameOver) {
                    logger.info("Space pressed - Restarting game");
                    gameOver = false;
                    setupGame();
                }
            } else if (e.getCode() == KeyCode.ESCAPE && isGameStarted && !gameOver) {
                togglePause();
            }
        });

        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }

    private void togglePause() {
        if (gameLoop != null) {
            if (!isGamePaused) {
                gameLoop.stop();
                isGamePaused = true;
                gameView.showPauseMenu();
            } else {
                gameLoop.start();
                isGamePaused = false;
                gameView.hidePauseMenu();
            }
        }
    }

    private void setupGame() {


// Reset game state

        asteroids.clear();
        bullets.clear();
        boss = null;
        score = 0;
        gameOver = false;

        // Initialize player at center of screen
        player = new PlayerShip(new Point2D(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        gameView.addGameObject(player);
        gameView.updateScore(score);
        gameView.updateLives(player.getLives());
        gameView.hideGameOver();  // Hide any existing game over/victory messages

        // Spawn initial asteroids
        spawnAsteroids(5);
        if (gameLoop != null) {
            gameLoop.start();
        }

        logger.info("Game setup completed");
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
            gameView.addBullet(bullet);
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
                    gameView.addBullet(bullet);
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
                gameView.removeBullet(bullet);
                return true;
            }
            return false;
        });
    }

    private void handlePlayerHit() {
        player.hit();
        gameView.updateLives(player.getLives());
        gameView.showExplosion(player.getPosition());

        if (!player.isAlive()) {
            gameOver = true;
            gameView.showGameOver(score);
            logger.info("Game Over! Final score: {}", score);
        }
    }

    private void handleAsteroidHit(Asteroid asteroid, Bullet bullet) {
        score += asteroid.getPoints();
        gameView.updateScore(score);

        // Remove asteroid and bullet
        asteroids.remove(asteroid);
        bullets.remove(bullet);
        gameView.removeGameObject(asteroid);
        gameView.removeBullet(bullet);

        gameView.showExplosion(asteroid.getPosition());
        logger.info("Asteroid destroyed! Score: {}", score);
    }

    private void handleBossHit(Bullet bullet) {
        boss.hit(10);
        bullets.remove(bullet);
        gameView.removeBullet(bullet);
        gameView.showExplosion(bullet.getPosition());

        if (!boss.isAlive()) {
            score += boss.getPoints();
            gameView.updateScore(score);
            gameView.removeGameObject(boss);
            gameView.showVictory(score);
            gameOver = true;
            logger.info("Boss defeated! Final score: {}", score);
        }
    }

    private void spawnAsteroids(int count) {
        for (int i = 0; i < count; i++) {
            Point2D position = getRandomSpawnPosition();
            Asteroid asteroid = new Asteroid(position, 1);
            asteroids.add(asteroid);
            gameView.addGameObject(asteroid);
        }
    }

    private void spawnBoss() {
        Point2D position = new Point2D(GAME_WIDTH / 2, 100);
        boss = new Boss(position);
        gameView.addGameObject(boss);
        logger.info("Boss spawned!");
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
            gameView.removeGameObject(player);
        }
        for (Asteroid asteroid : asteroids) {
            gameView.removeGameObject(asteroid);
        }
        for (Bullet bullet : bullets) {
            gameView.removeBullet(bullet);
        }
        if (boss != null) {
            gameView.removeGameObject(boss);
        }
    }
}