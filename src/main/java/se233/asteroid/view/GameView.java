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

public class GameView extends Pane {
    private static final Logger logger = LogManager.getLogger(GameView.class);

    // Constants
    public static final double DEFAULT_WIDTH = 800;
    public static final double DEFAULT_HEIGHT = 600;
    private static final long BULLET_COOLDOWN = 250_000_000L; // 250ms

    // Game components
    private final GameStage gameStage;
    private final List<Character> gameObjects;
    private final List<Bullet> bullets;
    private PlayerShip player;
    private Boss boss;

    // State tracking
    private boolean isGameStarted;
    private boolean isPaused;
    private double currentWidth;
    private double currentHeight;
    private int currentWave;
    private int score;
    private long lastBulletTime;

    public GameView() {
        // Initialize collections
        this.gameObjects = new ArrayList<>();
        this.bullets = new ArrayList<>();

        // Setup stage and size
        this.gameStage = new GameStage();
        this.currentWidth = DEFAULT_WIDTH;
        this.currentHeight = DEFAULT_HEIGHT;
        getChildren().add(gameStage);

        // Initialize state
        this.isGameStarted = false;
        this.isPaused = false;
        this.currentWave = 1;
        this.score = 0;

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
        for (Bullet bullet : bullets) {
            // Check asteroid collisions
            for (Character obj : gameObjects) {
                if (obj instanceof Asteroid && obj.isAlive() && bullet.collidesWith(obj)) {
                    handleAsteroidHit((Asteroid)obj);
                    bullet.setActive(false);
                    break;
                }
            }
            // Check boss collision
            if (boss != null && boss.isAlive() && bullet.collidesWith(boss)) {
                handleBossHit();
                bullet.setActive(false);
            }
        }

        // Check player collisions
        for (Character obj : gameObjects) {
            if (obj.isAlive() && player.collidesWith(obj)) {
                handlePlayerCollision();
                break;
            }
        }

        // Check boss collision with player
        if (boss != null && boss.isAlive() && player.collidesWith(boss)) {
            handlePlayerCollision();
        }

        // Remove inactive bullets
        bullets.removeIf(bullet -> !bullet.isAlive());
    }

    private void handleAsteroidHit(Asteroid asteroid) {
        asteroid.hit();
        gameStage.showExplosion(asteroid.getPosition());

        if (!asteroid.isAlive()) {
            if (asteroid.getSize() > 1) {
                for (Asteroid fragment : asteroid.split()) {
                    addGameObject(fragment);
                }
            }
            increaseScore(100 * asteroid.getSize());
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

    private void checkWaveCompletion() {
        boolean allAsteroidsDestroyed = gameObjects.stream()
                .filter(obj -> obj instanceof Asteroid)
                .noneMatch(Character::isAlive);

        if (allAsteroidsDestroyed && (boss == null || !boss.isAlive())) {
            startNextWave();
        }
    }

    private void startNextWave() {
        currentWave++;
        gameStage.updateWave(currentWave);

        if (currentWave % 5 == 0) {
            spawnBoss();
        } else {
            spawnAsteroids();
        }
    }

    private void spawnBoss() {
        Point2D spawnPos = new Point2D(DEFAULT_WIDTH/2, -50);
        boss = new Boss(spawnPos, currentWave);
        gameStage.addGameObject(boss);

    }

    private void spawnAsteroids() {
        int baseCount = 4;
        int additionalCount = (currentWave - 1) * 2;
        int totalCount = Math.min(baseCount + additionalCount, 12);

        for (int i = 0; i < totalCount; i++) {
            spawnAsteroid();
        }
    }

    private void spawnAsteroid() {
        double x = Math.random() * DEFAULT_WIDTH;
        double y = Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50;
        Point2D spawnPos = new Point2D(x, y);

        Asteroid asteroid = new Asteroid(spawnPos, 3);
        addGameObject(asteroid);
    }

    private void increaseScore(int points) {
        score += points;
        gameStage.updateScore(score);
    }

    // Public control methods
    public void startGame() {
        if (!isGameStarted) {
            isGameStarted = true;
            gameStage.hideStartMenu();

            player = new PlayerShip(new Point2D(DEFAULT_WIDTH/2, DEFAULT_HEIGHT/2));
            gameStage.addGameObject(player);

            spawnAsteroids();
            score = 0;
            currentWave = 1;

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
        gameObjects.clear();
        bullets.clear();
        boss = null;
        player = null;
        isGameStarted = false;
        isPaused = false;
        currentWave = 1;
        score = 0;
        gameStage.reset();
    }

    private void endGame() {
        isGameStarted = false;
        gameStage.showGameOver(score);
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
    //public void thrust() { if (player != null) player.thrust(); }
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
    public int getScore() { return score; }
    public Button getStartButton() { return gameStage.getStartButton(); }
    public Button getRestartButton() { return gameStage.getRestartButton(); }
    public Button getResumeButton() { return gameStage.getResumeButton(); }
}