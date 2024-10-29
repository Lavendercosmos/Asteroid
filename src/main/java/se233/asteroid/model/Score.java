package se233.asteroid.model;

import javafx.animation.ScaleTransition;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import se233.asteroid.view.GameStage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.effect.DropShadow;

public class Score {
    private static final Logger logger = LogManager.getLogger(Score.class);

    // Score constants
    public static final int ASTEROID_POINTS = 2;
    public static final int METEOR_POINTS = 1;
    public static final int REGULAR_ENEMY_POINTS = 3;
    public static final int SECOND_TIER_ENEMY_POINTS = 4;
    public static final int BOSS_POINTS = 10;

    private int currentScore;
    private Text scoreText;
    private final GameStage gameStage;

    public Score(GameStage gameStage) {
        this.currentScore = 0;
        this.gameStage = gameStage;
        initializeScoreDisplay();
        logger.info("Score system initialized");
    }

    private void initializeScoreDisplay() {
        scoreText = new Text(20, 40, "Score: 0");
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        scoreText.setFill(Color.YELLOW);
        scoreText.setStroke(Color.BLACK);
        scoreText.setStrokeWidth(1);

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(1.2);
        dropShadow.setOffsetX(1.0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.BLACK);
        scoreText.setEffect(dropShadow);

        gameStage.getUiLayer().getChildren().add(scoreText);
        gameStage.getUiLayer().getChildren().get(gameStage.getUiLayer().getChildren().size() - 1).toFront();
        updateDisplay();
        logger.debug("Score display initialized");
    }

    public void addPoints(int points) {
        this.currentScore += points;
        updateDisplay();
        showScorePopup(points);
        logger.info("Added {} points. New score: {}", points, currentScore);
    }

    public void addAsteroidPoints() {
        addPoints(ASTEROID_POINTS);
        logger.debug("Added asteroid points: {}", ASTEROID_POINTS);
    }

    public void addMeteorPoints() {
        addPoints(METEOR_POINTS);
        logger.debug("Added meteor points: {}", METEOR_POINTS);
    }

    public void addRegularEnemyPoints() {
        addPoints(REGULAR_ENEMY_POINTS);
        logger.debug("Added regular enemy points: {}", REGULAR_ENEMY_POINTS);
    }

    public void addSecondTierEnemyPoints() {
        addPoints(SECOND_TIER_ENEMY_POINTS);
        logger.debug("Added second tier enemy points: {}", SECOND_TIER_ENEMY_POINTS);
    }

    public void addBossPoints() {
        addPoints(BOSS_POINTS);
        logger.debug("Added boss points: {}", BOSS_POINTS);
    }

    private void showScorePopup(int points) {
        Text popup = new Text("+" + points);
        popup.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        popup.setFill(Color.GOLD);
        popup.setStroke(Color.BLACK);
        popup.setStrokeWidth(1);

        popup.setX(scoreText.getX() + 100);
        popup.setY(scoreText.getY());

        FadeTransition fade = new FadeTransition(Duration.seconds(1), popup);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        TranslateTransition move = new TranslateTransition(Duration.seconds(1), popup);
        move.setByY(-30);
        move.setByX(20);

        ParallelTransition animation = new ParallelTransition(popup, fade, move);
        animation.setOnFinished(e -> gameStage.getUiLayer().getChildren().remove(popup));

        gameStage.getUiLayer().getChildren().add(popup);
        animation.play();
    }

    public void updateDisplay() {
        if (scoreText != null) {
            scoreText.setText("Score: " + currentScore);

            ScaleTransition st = new ScaleTransition(Duration.millis(100), scoreText);
            st.setFromX(1.2);
            st.setFromY(1.2);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            logger.debug("Score display updated: {}", currentScore);
        }
    }

    public void reset() {
        currentScore = 0;
        updateDisplay();
        logger.info("Score reset to 0");
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public Text getScoreText() {
        return scoreText;
    }
}