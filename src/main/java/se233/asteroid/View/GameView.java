package se233.asteroid.View;  // Changed from View to view

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import se233.asteroid.model.Character;
import se233.asteroid.model.Bullet;
import javafx.scene.control.Label;

public class GameView extends Pane {
    private Group gameObjects;
    private Group effects;
    private Label scoreLabel;
    private Label livesLabel;
    private Label messageLabel;

    private VBox startMenuGroup;
    private VBox pauseMenuGroup;

    public GameView() {
        gameObjects = new Group();
        effects = new Group();

        scoreLabel = new Label("Score: 0");
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font("Arial", 20));
        scoreLabel.setTranslateX(10);
        scoreLabel.setTranslateY(10);

        livesLabel = new Label("Lives: 3");
        livesLabel.setTextFill(Color.WHITE);
        livesLabel.setFont(Font.font("Arial", 20));
        livesLabel.setTranslateX(10);
        livesLabel.setTranslateY(40);

        messageLabel = new Label("");
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setFont(Font.font("Arial", 40));
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        setupLabels();
        setupStartMenu();
        setupPauseMenu();

        getChildren().addAll(gameObjects, effects, scoreLabel, livesLabel, messageLabel,
                startMenuGroup, pauseMenuGroup);
    }

    private void setupLabels() {
        scoreLabel = new Label("Score: 0");
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font("Arial", 20));
        scoreLabel.setTranslateX(10);
        scoreLabel.setTranslateY(10);

        livesLabel = new Label("Lives: 3");
        livesLabel.setTextFill(Color.WHITE);
        livesLabel.setFont(Font.font("Arial", 20));
        livesLabel.setTranslateX(10);
        livesLabel.setTranslateY(40);

        messageLabel = new Label("");
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setFont(Font.font("Arial", 40));
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private void setupStartMenu() {
        startMenuGroup = new VBox(20); // 20 pixels spacing between elements
        startMenuGroup.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("ASTEROID GAME");
        titleLabel.setFont(Font.font("Arial", 50));
        titleLabel.setTextFill(Color.WHITE);

        Label controlsLabel = new Label(
                "Controls:\n" +
                        "WASD - Move\n" +
                        "Q/E - Rotate\n" +
                        "SPACE - Shoot\n" +
                        "ESC - Pause"
        );
        controlsLabel.setFont(Font.font("Arial", 20));
        controlsLabel.setTextFill(Color.WHITE);
        controlsLabel.setTextAlignment(TextAlignment.CENTER);

        Label startLabel = new Label("Press SPACE to Start");
        startLabel.setFont(Font.font("Arial", 30));
        startLabel.setTextFill(Color.YELLOW);

        startMenuGroup.getChildren().addAll(titleLabel, controlsLabel, startLabel);
        startMenuGroup.setVisible(true);
    }

    private void setupPauseMenu() {
        pauseMenuGroup = new VBox(20);
        pauseMenuGroup.setAlignment(Pos.CENTER);

        Label pauseLabel = new Label("GAME PAUSED");
        pauseLabel.setFont(Font.font("Arial", 50));
        pauseLabel.setTextFill(Color.WHITE);

        Label resumeLabel = new Label(
                "Press ESC to Resume\n" +
                        "Press R to Restart"
        );
        resumeLabel.setFont(Font.font("Arial", 20));
        resumeLabel.setTextFill(Color.WHITE);
        resumeLabel.setTextAlignment(TextAlignment.CENTER);

        pauseMenuGroup.getChildren().addAll(pauseLabel, resumeLabel);
        pauseMenuGroup.setVisible(false);
    }

    // Add these new methods
    public void hideStartMenu() {
        startMenuGroup.setVisible(false);
    }

    public void showPauseMenu() {
        pauseMenuGroup.setVisible(true);
    }

    public void hidePauseMenu() {
        pauseMenuGroup.setVisible(false);
    }

    public void addGameObject(Character character) {  // Changed from GameObject to Character
        if (character.getSprite() != null) {
            gameObjects.getChildren().add(character.getSprite());
        }
    }

    public void removeGameObject(Character character) {  // Changed from GameObject to Character
        if (character.getSprite() != null) {
            gameObjects.getChildren().remove(character.getSprite());
        }
    }

    public void addBullet(Bullet bullet) {
        if (bullet.getShape() != null) {
            gameObjects.getChildren().add(bullet.getShape());
            bullet.getShape().setFill(Color.RED);
        }
    }

    public void removeBullet(Bullet bullet) {
        if (bullet.getShape() != null) {
            gameObjects.getChildren().remove(bullet.getShape());
        }
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void updateLives(int lives) {
        livesLabel.setText("Lives: " + lives);
    }

    public void showGameOver(int finalScore) {
        showMessage("Game Over!\nScore: " + finalScore + "\nPress SPACE to restart");
    }

    public void showVictory(int finalScore) {
        showMessage("Victory!\nScore: " + finalScore + "\nPress SPACE to restart");
    }

    public void hideGameOver() {
        messageLabel.setVisible(false);
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);

        // Center the message
        double x = (getWidth() - messageLabel.getWidth()) / 2;
        double y = (getHeight() - messageLabel.getHeight()) / 2;
        messageLabel.setTranslateX(x);
        messageLabel.setTranslateY(y);
    }

    public void showExplosion(Point2D position) {
        // Create explosion effect
        Circle explosion = new Circle(position.getX(), position.getY(), 2, Color.YELLOW);
        effects.getChildren().add(explosion);

        // Animate the explosion
        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), explosion);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> effects.getChildren().remove(explosion));

        // Scale animation
        explosion.setScaleX(1);
        explosion.setScaleY(1);
        explosion.setScaleZ(1);

        explosion.setScaleX(20);
        explosion.setScaleY(20);
        explosion.setScaleZ(20);

        fade.play();
    }

    @Override
    public void resize(double width, double height) {
        super.resize(width, height);

        // Update message label position when window is resized
        if (messageLabel.isVisible()) {
            double x = (width - messageLabel.getWidth()) / 2;
            double y = (height - messageLabel.getHeight()) / 2;
            messageLabel.setTranslateX(x);
            messageLabel.setTranslateY(y);
        }
        startMenuGroup.setTranslateX((width - startMenuGroup.getWidth()) / 2);
        startMenuGroup.setTranslateY((height - startMenuGroup.getHeight()) / 2);

        pauseMenuGroup.setTranslateX((width - pauseMenuGroup.getWidth()) / 2);
        pauseMenuGroup.setTranslateY((height - pauseMenuGroup.getHeight()) / 2);
    }
}