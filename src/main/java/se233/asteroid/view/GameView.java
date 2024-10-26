package se233.asteroid.view;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;
import se233.asteroid.model.Character;
import se233.asteroid.model.PlayerShip;
import se233.asteroid.model.Bullet;
import se233.asteroid.model.Asteroid;
import se233.asteroid.model.Boss;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import javafx.animation.AnimationTimer;
import javafx.scene.transform.Scale;

public class GameView extends Pane {
    private static final Logger logger = LogManager.getLogger(GameView.class);
    public static final double DEFAULT_WIDTH = 800;
    public static final double DEFAULT_HEIGHT = 600;

    private GameStage gameStage;
    private PlayerShip player;
    private List<Bullet> bullets;
    private List<Asteroid> asteroids;
    private Boss boss;
    private boolean isGameStarted;
    private boolean isPaused;
    private AnimationTimer gameLoop;
    private double currentWidth;
    private double currentHeight;
    private int currentWave;
    private int score;
    private int difficultyLevel;
    private long lastBulletTime;
    private static long BULLET_COOLDOWN = 250_000_000; // 250ms in nanoseconds

    public GameView() {
        currentWidth = DEFAULT_WIDTH;
        currentHeight = DEFAULT_HEIGHT;
        currentWave = 1;
        score = 0;
        difficultyLevel = 1;

        gameStage = new GameStage();
        bullets = new ArrayList<>();
        asteroids = new ArrayList<>();
        getChildren().add(gameStage);

        isGameStarted = false;
        isPaused = false;

        setupGameLoop();
        setupButtonHandlers();

        logger.info("GameView initialized");
    }

    private void setupButtonHandlers() {
        // Get buttons from GameStage
        Button startButton = gameStage.getStartButton();
        Button restartButton = gameStage.getRestartButton();
        Button resumeButton = gameStage.getResumeButton();

        // Setup handlers
        startButton.setOnAction(e -> startGame());
        restartButton.setOnAction(e -> reset());
        resumeButton.setOnAction(e -> resumeGame());
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPaused && isGameStarted) {
                    updateGame(now);
                    checkWaveCompletion();
                }
            }
        };
    }

    private void updateGame(long now) {
        if (player != null) {
            updatePlayer();
            updateBullets();
            updateAsteroids();
            updateBoss();
            checkCollisions();
            updateGameDifficulty();
        }
    }

    private void updatePlayer() {
        if (player.isAlive()) {
            player.update();
            wrapAround(player);

            // Update player's screen position
            Point2D gamePos = gameStage.screenToGame(player.getPosition());
            Point2D screenPos = gameStage.gameToScreen(gamePos);
            player.setPosition(screenPos);
        }
    }

    private void updateBullets() {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update();

            Point2D gamePos = gameStage.screenToGame(bullet.getPosition());
            if (isOffScreen(gamePos)) {
                gameStage.removeBullet(bullet);
                bulletIterator.remove();
            } else {
                // Update bullet's screen position
                Point2D screenPos = gameStage.gameToScreen(gamePos);
                bullet.setPosition(screenPos);
            }
        }
    }

    private void updateAsteroids() {
        Iterator<Asteroid> asteroidIterator = asteroids.iterator();
        while (asteroidIterator.hasNext()) {
            Asteroid asteroid = asteroidIterator.next();
            if (asteroid.isAlive()) {
                asteroid.update();
                wrapAround(asteroid);

                // Update asteroid's screen position
                Point2D gamePos = gameStage.screenToGame(asteroid.getPosition());
                Point2D screenPos = gameStage.gameToScreen(gamePos);
                asteroid.setPosition(screenPos);
            } else {
                gameStage.removeGameObject(asteroid);
                asteroidIterator.remove();
                increaseScore(100 * asteroid.getSize());
            }
        }
    }

    private void updateBoss() {
        if (boss != null && boss.isAlive()) {
            Point2D playerPos = gameStage.screenToGame(player.getPosition());
            boss.update(0.016, playerPos);

            // Update boss's screen position
            Point2D gamePos = gameStage.screenToGame(boss.getPosition());
            Point2D screenPos = gameStage.gameToScreen(gamePos);
            boss.setPosition(screenPos);
        }
    }

    private void checkCollisions() {
        // Convert positions to game coordinates for collision checks
        Point2D playerGamePos = gameStage.screenToGame(player.getPosition());

        // Check bullet collisions
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            Point2D bulletGamePos = gameStage.screenToGame(bullet.getPosition());

            // Check asteroid collisions
            Iterator<Asteroid> asteroidIterator = asteroids.iterator();
            boolean bulletHit = false;

            while (asteroidIterator.hasNext() && !bulletHit) {
                Asteroid asteroid = asteroidIterator.next();
                Point2D asteroidGamePos = gameStage.screenToGame(asteroid.getPosition());

                if (checkCollision(bulletGamePos, asteroidGamePos, bullet.getRadius(), asteroid.getRadius())) {
                    handleAsteroidHit(asteroid);
                    gameStage.removeBullet(bullet);
                    bulletIterator.remove();
                    bulletHit = true;
                }
            }

            // Check boss collision
            if (!bulletHit && boss != null && boss.isAlive()) {
                Point2D bossGamePos = gameStage.screenToGame(boss.getPosition());
                if (checkCollision(bulletGamePos, bossGamePos, bullet.getRadius(), boss.getRadius())) {
                    handleBossHit();
                    gameStage.removeBullet(bullet);
                    bulletIterator.remove();
                }
            }
        }

        // Check player collisions with asteroids
        if (player.isAlive() && !player.isInvulnerable()) {
            for (Asteroid asteroid : asteroids) {
                Point2D asteroidGamePos = gameStage.screenToGame(asteroid.getPosition());
                if (checkCollision(playerGamePos, asteroidGamePos, player.getRadius(), asteroid.getRadius())
                        && !asteroid.isExploding()) {
                    handlePlayerHit();
                }
            }

            // Check player collision with boss
            if (boss != null && boss.isAlive()) {
                Point2D bossGamePos = gameStage.screenToGame(boss.getPosition());
                if (checkCollision(playerGamePos, bossGamePos, player.getRadius(), boss.getRadius())) {
                    handlePlayerHit();
                }
            }
        }
    }

    private boolean checkCollision(Point2D pos1, Point2D pos2, double radius1, double radius2) {
        double distance = pos1.distance(pos2);
        return distance < (radius1 + radius2);
    }

    private void handleAsteroidHit(Asteroid asteroid) {
        asteroid.explode();
        gameStage.showExplosion(asteroid.getPosition());

        if (asteroid.getSize() > 1) {
            List<Asteroid> fragments = asteroid.split();
            for (Asteroid fragment : fragments) {
                addAsteroid(fragment);
            }
        }

        increaseScore(100 * asteroid.getSize());
    }

    private void handleBossHit() {
        boss.hit(10); // or whatever damage value you want bullets to deal
        if (!boss.isAlive()) {
            gameStage.showExplosion(boss.getPosition());
            increaseScore(5000);
            startNextWave();
        }
    }

    private void handlePlayerHit() {
        player.hit();
        gameStage.updateLives(player.getLives());
        gameStage.showExplosion(player.getPosition());

        if (!player.isAlive()) {
            endGame();
        }
    }

    private void wrapAround(Character character) {
        Point2D screenPos = character.getPosition();
        Point2D gamePos = gameStage.screenToGame(screenPos);
        double x = gamePos.getX();
        double y = gamePos.getY();
        boolean wrapped = false;

        if (x < 0) {
            x = DEFAULT_WIDTH;
            wrapped = true;
        }
        if (x > DEFAULT_WIDTH) {
            x = 0;
            wrapped = true;
        }
        if (y < 0) {
            y = DEFAULT_HEIGHT;
            wrapped = true;
        }
        if (y > DEFAULT_HEIGHT) {
            y = 0;
            wrapped = true;
        }

        if (wrapped) {
            Point2D newGamePos = new Point2D(x, y);
            Point2D newScreenPos = gameStage.gameToScreen(newGamePos);
            character.setPosition(newScreenPos);
        }
    }

    private boolean isOffScreen(Point2D position) {
        return position.getX() < -50 || position.getX() > DEFAULT_WIDTH + 50 ||
                position.getY() < -50 || position.getY() > DEFAULT_HEIGHT + 50;
    }

    private void checkWaveCompletion() {
        if (asteroids.isEmpty() && (boss == null || !boss.isAlive())) {
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

        updateGameDifficulty();
    }

    private void spawnBoss() {
        // Create boss at a random edge position
        double x = Math.random() < 0.5 ? 0 : DEFAULT_WIDTH;
        double y = Math.random() * DEFAULT_HEIGHT;
        Point2D spawnPos = gameStage.gameToScreen(new Point2D(x, y));

        boss = new Boss(spawnPos, currentWave);
        gameStage.addGameObject(boss);
    }

    private void spawnAsteroids() {
        int baseCount = 4;
        int additionalCount = (currentWave - 1) * 2;
        int totalCount = Math.min(baseCount + additionalCount, 12); // Maximum 12 asteroids

        for (int i = 0; i < totalCount; i++) {
            spawnAsteroid();
        }
    }

    private void spawnAsteroid() {
        // Spawn asteroid at random edge position
        double x, y;
        if (Math.random() < 0.5) {
            x = Math.random() < 0.5 ? -50 : DEFAULT_WIDTH + 50;
            y = Math.random() * DEFAULT_HEIGHT;
        } else {
            x = Math.random() * DEFAULT_WIDTH;
            y = Math.random() < 0.5 ? -50 : DEFAULT_HEIGHT + 50;
        }

        Point2D spawnPos = gameStage.gameToScreen(new Point2D(x, y));
        Asteroid asteroid = new Asteroid(spawnPos, 3); // Size 3 is largest
        addAsteroid(asteroid);
    }

    private void updateGameDifficulty() {
        difficultyLevel = 1 + (currentWave - 1) / 3;
        // Update game speed and other difficulty parameters based on difficultyLevel
    }

    private void increaseScore(int points) {
        score += points * difficultyLevel;
        gameStage.updateScore(score);
    }

    // Public control methods
    public void startGame() {
        if (!isGameStarted) {
            // Create player at center of screen
            Point2D centerGame = new Point2D(DEFAULT_WIDTH/2, DEFAULT_HEIGHT/2);
            Point2D centerScreen = gameStage.gameToScreen(centerGame);
            player = new PlayerShip(centerScreen);
            gameStage.addGameObject(player);

            // Initialize first wave
            currentWave = 1;
            score = 0;
            spawnAsteroids();

            isGameStarted = true;
            gameStage.hideStartMenu();
            gameLoop.start();

            logger.info("Game started");
        }
    }

    public void pauseGame() {
        if (isGameStarted && !isPaused) {
            isPaused = true;
            gameStage.showPauseMenu();
            logger.info("Game paused");
        }
    }

    public void resumeGame() {
        if (isPaused) {
            isPaused = false;
            gameStage.hidePauseMenu();
            logger.info("Game resumed");
        }
    }

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

    private void endGame() {
        isGameStarted = false;
        gameLoop.stop();
        gameStage.showGameOver(score);
        logger.info("Game over with score: {}", score);
    }

    public void reset() {
        // Clear all game objects
        bullets.clear();
        asteroids.clear();
        boss = null;

        // Reset player
        if (player != null) {
            player = null;
        }

        // Reset game state
        isGameStarted = false;
        isPaused = false;
        currentWave = 1;
        score = 0;
        difficultyLevel = 1;
        lastBulletTime = 0;

        // Reset UI
        gameStage.reset();

        logger.info("Game reset");
    }

    // Movement controls
    public void moveLeft() {
        if (player != null) player.moveLeft();
    }

    public void moveRight() {
        if (player != null) player.moveRight();
    }

    public void moveUp() {
        if (player != null) player.moveUp();
    }

    public void moveDown() {
        if (player != null) player.moveDown();
    }

    public void rotateLeft() {
        if (player != null) player.rotateLeft();
    }

    public void rotateRight() {
        if (player != null) player.rotateRight();
    }

    public void thrust() {
        if (player != null) player.thrust();
    }
    public void stopThrust() {
        if (player != null) player.stopThrust();
    }

    // Resize handling
    public void handleResize(double width, double height) {
        currentWidth = width;
        currentHeight = height;
        gameStage.handleResize(width, height);

        if (player != null) {
            updateObjectPosition(player);
        }

        for (Bullet bullet : bullets) {
            updateObjectPosition(bullet);
        }

        for (Asteroid asteroid : asteroids) {
            updateObjectPosition(asteroid);
        }

        if (boss != null) {
            updateObjectPosition(boss);
        }

        logger.debug("Game view resized to: {}x{}", width, height);
    }

    private void updateObjectPosition(Character gameObject) {
        Point2D gamePos = gameStage.screenToGame(gameObject.getPosition());
        Point2D screenPos = gameStage.gameToScreen(gamePos);
        gameObject.setPosition(screenPos);
    }

    // Getters for GameStage controls
    public Button getStartButton() {
        return gameStage.getStartButton();
    }

    public Button getRestartButton() {
        return gameStage.getRestartButton();
    }

    public Button getResumeButton() {
        return gameStage.getResumeButton();
    }

    // Game state getters
    public boolean isGameStarted() {
        return isGameStarted;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getScore() {
        return score;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public static double getDefaultWidth() {
        return DEFAULT_WIDTH;
    }

    public static double getDefaultHeight() {
        return DEFAULT_HEIGHT;
    }

    public double getCurrentWidth() {
        return currentWidth;
    }

    public double getCurrentHeight() {
        return currentHeight;
    }

    // Game state updates
    public void addAsteroid(Asteroid asteroid) {
        asteroids.add(asteroid);
        gameStage.addGameObject(asteroid);
        logger.debug("Added asteroid at position: {}", asteroid.getPosition());
    }

    public void removeAsteroid(Asteroid asteroid) {
        asteroids.remove(asteroid);
        gameStage.removeGameObject(asteroid);
        logger.debug("Removed asteroid at position: {}", asteroid.getPosition());
    }

    // Power-up and special effects methods
    public void activateShield() {
        if (player != null) {
            player.activateShield();
            // Add visual effect
            gameStage.showShieldEffect(player.getPosition());
        }
    }

    public void activateRapidFire() {
        if (player != null) {
            BULLET_COOLDOWN = 125_000_000; // 125ms cooldown
            // Start timer to reset cooldown
            new AnimationTimer() {
                private long startTime = System.nanoTime();
                @Override
                public void handle(long now) {
                    if (now - startTime >= 5_000_000_000L) { // 5 seconds
                        BULLET_COOLDOWN = 250_000_000; // Reset to normal
                        this.stop();
                    }
                }
            }.start();
        }
    }


//    // Debug methods
//    public void toggleDebugMode() {
//        // Add debug visualization
//        for (Asteroid asteroid : asteroids) {
//            asteroid.toggleDebugView();
//        }
//        if (player != null) {
//            player.toggleDebugView();
//        }
//        if (boss != null) {
//            boss.toggleDebugView();
//        }
//    }

    // Additional helper methods
    private void spawnPowerUp(Point2D position) {
        // Implementation for power-up spawning
        logger.debug("Power-up spawned at position: {}", position);
    }

    private void handlePowerUpCollection(String type) {
        switch (type) {
            case "SHIELD":
                activateShield();
                break;
            case "RAPID_FIRE":
                activateRapidFire();
                break;
            case "EXTRA_LIFE":
                if (player != null) {
                    player.addLife();
                    gameStage.updateLives(player.getLives());
                }
                break;
            default:
                logger.warn("Unknown power-up type: {}", type);
        }
    }

    // Sound effect methods
    public void playSound(String soundEffect) {
        switch (soundEffect) {
            case "SHOOT":
                // Play shoot sound
                break;
            case "EXPLOSION":
                // Play explosion sound
                break;
            case "POWER_UP":
                // Play power-up sound
                break;
            default:
                logger.warn("Unknown sound effect: {}", soundEffect);
        }
    }

    // Clean up resources
    public void dispose() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Clear all collections
        bullets.clear();
        asteroids.clear();

        // Clean up player
        if (player != null) {
            player = null;
        }

        // Clean up boss
        if (boss != null) {
            boss = null;
        }

        // Remove all children
        getChildren().clear();

        logger.info("Game view disposed");
    }
}
