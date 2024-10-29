import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import se233.asteroid.model.Score;
import se233.asteroid.view.GameStage;
import javafx.scene.text.Text;
import javafx.application.Platform;

import org.junit.jupiter.api.BeforeAll;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ScoreTest {
    private Score score;
    private GameStage gameStage;
    private static final long JAVAFX_TIMEOUT = 5;

    @BeforeEach
    void setUp() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    gameStage = new GameStage();
                    score = new Score(gameStage);
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(JAVAFX_TIMEOUT, TimeUnit.SECONDS)) {
                throw new RuntimeException("Setup timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Test setup was interrupted", e);
        }
    }

    private void runAndWait(Runnable action) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    action.run();
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(JAVAFX_TIMEOUT, TimeUnit.SECONDS)) {
                throw new RuntimeException("JavaFX operation timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Test was interrupted", e);
        }
    }

    @Test
    void testInitialScore() {
        runAndWait(() -> {
            assertEquals(0, score.getCurrentScore(), "Initial score should be 0");
            Text scoreText = score.getScoreText();
            assertNotNull(scoreText, "Score text should be initialized");
            assertEquals("Score: 0", scoreText.getText(), "Initial score display should be 'Score: 0'");
        });
    }

    @Test
    void testAddAsteroidPoints() {
        runAndWait(() -> {
            score.addAsteroidPoints();
            assertEquals(Score.ASTEROID_POINTS, score.getCurrentScore(),
                    "Score should increase by asteroid points");
            assertEquals("Score: " + Score.ASTEROID_POINTS, score.getScoreText().getText(),
                    "Score display should reflect asteroid points");
        });
    }

    @Test
    void testAddMeteorPoints() {
        runAndWait(() -> {
            score.addMeteorPoints();
            assertEquals(Score.METEOR_POINTS, score.getCurrentScore(),
                    "Score should increase by meteor points");
        });
    }

    @Test
    void testAddRegularEnemyPoints() {
        runAndWait(() -> {
            score.addRegularEnemyPoints();
            assertEquals(Score.REGULAR_ENEMY_POINTS, score.getCurrentScore(),
                    "Score should increase by regular enemy points");
        });
    }

    @Test
    void testAddSecondTierEnemyPoints() {
        runAndWait(() -> {
            score.addSecondTierEnemyPoints();
            assertEquals(Score.SECOND_TIER_ENEMY_POINTS, score.getCurrentScore(),
                    "Score should increase by second tier enemy points");
        });
    }

    @Test
    void testAddBossPoints() {
        runAndWait(() -> {
            score.addBossPoints();
            assertEquals(Score.BOSS_POINTS, score.getCurrentScore(),
                    "Score should increase by boss points");
        });
    }

    @Test
    void testMultiplePointAdditions() {
        runAndWait(() -> {
            score.addAsteroidPoints();
            score.addMeteorPoints();
            score.addBossPoints();

            int expectedScore = Score.ASTEROID_POINTS + Score.METEOR_POINTS + Score.BOSS_POINTS;
            assertEquals(expectedScore, score.getCurrentScore(),
                    "Score should correctly sum multiple point additions");
        });
    }

    @Test
    void testReset() {
        runAndWait(() -> {
            score.addBossPoints();
            score.addAsteroidPoints();
            score.reset();

            assertEquals(0, score.getCurrentScore(), "Score should be 0 after reset");
            assertEquals("Score: 0", score.getScoreText().getText(),
                    "Score display should show 0 after reset");
        });
    }

    @Test
    void testCustomPointAddition() {
        runAndWait(() -> {
            int customPoints = 5;
            score.addPoints(customPoints);
            assertEquals(customPoints, score.getCurrentScore(),
                    "Score should increase by custom point value");
        });
    }
}
