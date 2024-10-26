package se233.asteroid.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

public class GameState {
    private static final Logger logger = LogManager.getLogger(GameState.class);

    // Game state properties with binding support
    private final BooleanProperty gameStarted = new SimpleBooleanProperty(false);
    private final BooleanProperty gamePaused = new SimpleBooleanProperty(false);
    private final BooleanProperty gameOver = new SimpleBooleanProperty(false);
    private final IntegerProperty currentWave = new SimpleIntegerProperty(1);
    private final IntegerProperty score = new SimpleIntegerProperty(0);
    private final IntegerProperty lives = new SimpleIntegerProperty(3);
    private final IntegerProperty highScore = new SimpleIntegerProperty(0);

    // Game progression constants
    private static final int STARTING_LIVES = 3;
    private static final int POINTS_PER_ASTEROID = 100;
    private static final int POINTS_PER_BOSS = 1000;
    private static final int WAVE_BONUS = 500;
    private static final int EXTRA_LIFE_SCORE = 10000;

    // Game difficulty scaling
    private static final int BASE_ASTEROIDS_PER_WAVE = 4;
    private static final int MAX_ASTEROIDS_PER_WAVE = 12;
    private static final int BOSS_WAVE_INTERVAL = 5;

    // Collections for game objects
    private final List<Asteroid> asteroids = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private Boss currentBoss;
    private PlayerShip player;

    // Wave management
    private int lastExtraLifeScore = 0;
    private long waveStartTime;
    private boolean waveInProgress;

    public GameState() {
        setupInitialState();
        setupScoreListener();
    }

    private void setupInitialState() {
        lives.set(STARTING_LIVES);
        score.set(0);
        currentWave.set(1);
        waveStartTime = System.currentTimeMillis();
        waveInProgress = false;
        logger.info("Game state initialized with {} lives", STARTING_LIVES);
    }

    private void setupScoreListener() {
        score.addListener((observable, oldValue, newValue) -> {
            // Check for extra life
            if (newValue.intValue() / EXTRA_LIFE_SCORE > lastExtraLifeScore / EXTRA_LIFE_SCORE) {
                awardExtraLife();
                lastExtraLifeScore = newValue.intValue();
            }
            // Update high score
            if (newValue.intValue() > highScore.get()) {
                highScore.set(newValue.intValue());
            }
        });
    }

    // Game state control methods
    public void startGame() {
        if (!gameStarted.get()) {
            gameStarted.set(true);
            gamePaused.set(false);
            gameOver.set(false);
            setupInitialState();
            logger.info("Game started");
        }
    }

    public void pauseGame() {
        if (gameStarted.get() && !gameOver.get()) {
            gamePaused.set(!gamePaused.get());
            logger.debug("Game {} state", gamePaused.get() ? "paused" : "resumed");
        }
    }

    public void endGame() {
        gameStarted.set(false);
        gameOver.set(true);
        logger.info("Game over with final score: {}", score.get());
    }

    // Wave management methods
    public void startNewWave() {
        currentWave.set(currentWave.get() + 1);
        waveStartTime = System.currentTimeMillis();
        waveInProgress = true;
        clearWaveEntities();
        spawnWaveEntities();
        awardWaveBonus();

        logger.info("Starting wave {} with {} asteroids",
                currentWave.get(), calculateAsteroidsForWave());
    }

    private void clearWaveEntities() {
        asteroids.clear();
        powerUps.clear();
        currentBoss = null;
    }

    private void spawnWaveEntities() {
        if (currentWave.get() % BOSS_WAVE_INTERVAL == 0) {
            spawnBossWave();
        } else {
            spawnAsteroidWave();
        }
    }

    private void spawnAsteroidWave() {
        int asteroidCount = calculateAsteroidsForWave();
        for (int i = 0; i < asteroidCount; i++) {
            Asteroid asteroid = createRandomAsteroid();
            asteroids.add(asteroid);
        }
    }

    private void spawnBossWave() {
        // สร้างตำแหน่งเริ่มต้นสำหรับ Boss (ตัวอย่างเช่น กลางจอด้านบน)
        Point2D bossStartPosition = new Point2D(
                se233.asteroid.view.GameStage.WINDOW_WIDTH / 2,  // ตำแหน่ง x กลางจอ
                100  // ตำแหน่ง y ด้านบนจอ
        );

        // สร้าง Boss object โดยส่ง Point2D และ wave number ปัจจุบัน
        currentBoss = new Boss(bossStartPosition, getCurrentWave());
        logger.info("Boss spawned for wave {} at position {}", currentWave.get(), bossStartPosition);
    }

    private Asteroid createRandomAsteroid() {
        // Create asteroid with random position and size
        // Implementation details would go here
        return null; // Placeholder
    }

    // Scoring methods
    public void addScore(int points) {
        score.set(score.get() + points);
        logger.debug("Score increased by {} to {}", points, score.get());
    }

    public void awardWaveBonus() {
        int bonus = WAVE_BONUS * currentWave.get();
        addScore(bonus);
        logger.info("Wave bonus awarded: {}", bonus);
    }

    private void awardExtraLife() {
        lives.set(lives.get() + 1);
        logger.info("Extra life awarded at score {}", score.get());
    }

    // Player management methods
    public void handlePlayerHit() {
        if (lives.get() > 0) {
            lives.set(lives.get() - 1);
            if (lives.get() <= 0) {
                endGame();
            }
            logger.info("Player hit, lives remaining: {}", lives.get());
        }
    }

    public boolean isWaveComplete() {
        if (!waveInProgress) {
            return false;
        }

        boolean complete = asteroids.isEmpty() &&
                (currentBoss == null || !currentBoss.isAlive());

        if (complete) {
            waveInProgress = false;
            logger.info("Wave {} completed", currentWave.get());
        }

        return complete;
    }

    // Difficulty scaling calculations
    private int calculateAsteroidsForWave() {
        int baseCount = BASE_ASTEROIDS_PER_WAVE;
        int additionalCount = (currentWave.get() - 1) * 2;
        return Math.min(baseCount + additionalCount, MAX_ASTEROIDS_PER_WAVE);
    }

    private int calculateBossHealth() {
        return 100 + (currentWave.get() * 50);
    }

    private int calculateBossDamage() {
        return 10 + (currentWave.get() * 5);
    }

    // Getters for properties
    public BooleanProperty gameStartedProperty() { return gameStarted; }
    public BooleanProperty gamePausedProperty() { return gamePaused; }
    public BooleanProperty gameOverProperty() { return gameOver; }
    public IntegerProperty currentWaveProperty() { return currentWave; }
    public IntegerProperty scoreProperty() { return score; }
    public IntegerProperty livesProperty() { return lives; }
    public IntegerProperty highScoreProperty() { return highScore; }

    // Getters for values
    public boolean isGameStarted() { return gameStarted.get(); }
    public boolean isGamePaused() { return gamePaused.get(); }
    public boolean isGameOver() { return gameOver.get(); }
    public int getCurrentWave() { return currentWave.get(); }
    public int getScore() { return score.get(); }
    public int getLives() { return lives.get(); }
    public int getHighScore() { return highScore.get(); }
    public List<Asteroid> getAsteroids() { return new ArrayList<>(asteroids); }
    public List<PowerUp> getPowerUps() { return new ArrayList<>(powerUps); }
    public Boss getCurrentBoss() { return currentBoss; }
    public PlayerShip getPlayer() { return player; }
    public boolean isWaveInProgress() { return waveInProgress; }

    // Reset method
    public void reset() {
        setupInitialState();
        clearWaveEntities();
        gameStarted.set(false);
        gamePaused.set(false);
        gameOver.set(false);
        logger.info("Game state reset");
    }

    // PowerUp class (Inner class or could be separate file)
    public static class PowerUp {
        public enum Type {
            SHIELD,
            RAPID_FIRE,
            EXTRA_LIFE,
            SCORE_MULTIPLIER
        }

        private final Type type;
        private final long duration;

        public PowerUp(Type type, long duration) {
            this.type = type;
            this.duration = duration;
        }

        public Type getType() { return type; }
        public long getDuration() { return duration; }
    }
}