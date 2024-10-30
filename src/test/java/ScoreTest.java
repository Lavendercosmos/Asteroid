import javafx.scene.Group;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import se233.asteroid.model.Score;
import se233.asteroid.view.GameStage;
import static org.junit.jupiter.api.Assertions.*;

public class ScoreTest {
    private Score score;
    private TestGameStage gameStage;

    // Custom TestGameStage class for testing
    private static class TestGameStage extends GameStage {
        private final Group uiLayer;

        public TestGameStage() {
            super();
            this.uiLayer = new Group();
        }

    }

    @BeforeEach
    public void setUp() {
        // Create a test GameStage and initialize Score
        gameStage = new TestGameStage();
        score = new Score(gameStage);
    }

    @Test
    @DisplayName("Test initial score is zero")
    public void testInitialScore() {
        assertEquals(0, score.getCurrentScore(), "Initial score should be 0");
    }

    @Test
    @DisplayName("Test adding asteroid points")
    public void testAddAsteroidPoints() {
        score.addAsteroidPoints();
        assertEquals(Score.ASTEROID_POINTS, score.getCurrentScore(),
                "Score should increase by asteroid points value");
    }

    @Test
    @DisplayName("Test adding meteor points")
    public void testAddMeteorPoints() {
        score.addMeteorPoints();
        assertEquals(Score.METEOR_POINTS, score.getCurrentScore(),
                "Score should increase by meteor points value");
    }

    @Test
    @DisplayName("Test adding regular enemy points")
    public void testAddRegularEnemyPoints() {
        score.addRegularEnemyPoints();
        assertEquals(Score.REGULAR_ENEMY_POINTS, score.getCurrentScore(),
                "Score should increase by regular enemy points value");
    }

    @Test
    @DisplayName("Test adding second tier enemy points")
    public void testAddSecondTierEnemyPoints() {
        score.addSecondTierEnemyPoints();
        assertEquals(Score.SECOND_TIER_ENEMY_POINTS, score.getCurrentScore(),
                "Score should increase by second tier enemy points value");
    }

    @Test
    @DisplayName("Test adding boss points")
    public void testAddBossPoints() {
        score.addBossPoints();
        assertEquals(Score.BOSS_POINTS, score.getCurrentScore(),
                "Score should increase by boss points value");
    }

    @Test
    @DisplayName("Test adding multiple different points")
    public void testAddMultiplePoints() {
        score.addAsteroidPoints();
        score.addMeteorPoints();
        score.addBossPoints();

        int expectedTotal = Score.ASTEROID_POINTS + Score.METEOR_POINTS + Score.BOSS_POINTS;
        assertEquals(expectedTotal, score.getCurrentScore(),
                "Score should be the sum of all added points");
    }

    @Test
    @DisplayName("Test score reset")
    public void testScoreReset() {
        score.addPoints(100);
        score.reset();
        assertEquals(0, score.getCurrentScore(),
                "Score should be 0 after reset");
    }

    @Test
    @DisplayName("Test score text display")
    public void testScoreTextDisplay() {
        Text scoreText = score.getScoreText();
        assertNotNull(scoreText, "Score text should not be null");
        assertEquals("Score: 0", scoreText.getText(),
                "Initial score text should display 'Score: 0'");

        score.addPoints(5);
        assertEquals("Score: 5", scoreText.getText(),
                "Score text should update when points are added");
    }

    @Test
    @DisplayName("Test adding custom points amount")
    public void testAddCustomPoints() {
        int customPoints = 15;
        score.addPoints(customPoints);
        assertEquals(customPoints, score.getCurrentScore(),
                "Score should increase by custom points value");
    }

    @Test
    @DisplayName("Test score text UI elements")
    public void testScoreTextUIElements() {
        Text scoreText = score.getScoreText();
        assertTrue(gameStage.getUiLayer().getChildren().contains(scoreText),
                "Score text should be added to UI layer");
    }

    @Test
    @DisplayName("Test consecutive score updates")
    public void testConsecutiveScoreUpdates() {
        score.addPoints(5);
        score.addPoints(10);
        score.addPoints(15);
        assertEquals(30, score.getCurrentScore(),
                "Score should accurately track consecutive updates");
        assertEquals("Score: 30", score.getScoreText().getText(),
                "Score text should reflect total after consecutive updates");
    }
}