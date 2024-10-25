package se233.asteroid.display;

import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.util.Duration;
import javafx.scene.control.Button;
import se233.asteroid.model.Character;
import se233.asteroid.model.Bullet;
import se233.asteroid.model.Boss;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class GameStage extends Pane {
    private static final Logger logger = LogManager.getLogger(GameStage.class);

    private ImageView backgroundView;
    private ImageView starsView;
    private Pane gameLayer;
    private Pane effectLayer;
    private Pane uiLayer;
    private Text scoreText;
    private Text livesText;
    private Text waveText;
    private Group gameOverGroup;
    private Group startMenuGroup;
    private Group pauseMenuGroup;
    private boolean isGameStarted;
    private boolean isPaused;
    private Button startButton;
    private Button restartButton;

    // Window size constants
    public static final double WINDOW_WIDTH = 800;
    public static final double WINDOW_HEIGHT = 600;

    public GameStage() {
        setupBackground();
        setupLayers();
        setupUI();
        setupStartMenu();
        setupPauseMenu();
        isGameStarted = false;
        isPaused = false;

        // Set preferred size
        setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        logger.info("GameStage initialized with dimensions: {}x{}", WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", 20));
        button.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15 30; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;"
        );

        // Hover effects
        button.setOnMouseEntered(e ->
                button.setStyle(button.getStyle() + "-fx-background-color: #45a049;"));

        button.setOnMouseExited(e ->
                button.setStyle(button.getStyle() + "-fx-background-color: #4CAF50;"));

        // Add shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setRadius(5.0);
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        button.setEffect(shadow);

        return button;
    }


    private void setupBackground() {
        try {
            // Load GIF file
            Image gifImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/se233/asteroid/assets/Backgrounds/SpaceBG.gif")));
            backgroundView = new ImageView(gifImage);

            // Make GIF resize with window
            backgroundView.fitWidthProperty().bind(this.widthProperty());
            backgroundView.fitHeightProperty().bind(this.heightProperty());
            backgroundView.setPreserveRatio(false); // ให้ GIF ยืดเต็มหน้าจอ

            // Add some star overlay effects for additional depth
            Pane starsOverlay = new Pane();
            starsOverlay.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

            // Add some animated stars
            for (int i = 0; i < 30; i++) {
                Text star = new Text("✦");
                star.setFill(Color.WHITE);
                star.setOpacity(Math.random() * 0.5 + 0.2); // ความโปร่งใสแบบสุ่ม
                star.setX(Math.random() * WINDOW_WIDTH);
                star.setY(Math.random() * WINDOW_HEIGHT);
                star.setFont(Font.font("Arial", 8 + Math.random() * 4));

                // Add twinkling animation
                FadeTransition twinkle = new FadeTransition(Duration.seconds(1 + Math.random() * 2), star);
                twinkle.setFromValue(star.getOpacity());
                twinkle.setToValue(star.getOpacity() * 0.3);
                twinkle.setCycleCount(FadeTransition.INDEFINITE);
                twinkle.setAutoReverse(true);
                twinkle.play();

                starsOverlay.getChildren().add(star);
            }

            starsOverlay.setMouseTransparent(true); // ให้คลิกผ่านได้

            getChildren().addAll(backgroundView, starsOverlay);

        } catch (Exception e) {
            logger.error("Failed to load background GIF", e);
            // Fallback to gradient background if GIF fails to load
            setStyle("-fx-background-color: linear-gradient(to bottom, #000022, #000066);");
        }
    }

    private void setupLayers() {
        gameLayer = new Pane();
        gameLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        effectLayer = new Pane();
        effectLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        effectLayer.setMouseTransparent(true);

        uiLayer = new Pane();
        uiLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        getChildren().addAll(gameLayer, effectLayer, uiLayer);
    }

    private void setupUI() {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLUE);
        dropShadow.setRadius(5);

        // Score display
        scoreText = new Text(10, 30, "Score: 0");
        scoreText.setFill(Color.WHITE);
        scoreText.setFont(Font.font("Arial Bold", 20));
        scoreText.setEffect(dropShadow);

        // Lives display
        livesText = new Text(10, 60, "Lives: 3");
        livesText.setFill(Color.WHITE);
        livesText.setFont(Font.font("Arial Bold", 20));
        livesText.setEffect(dropShadow);

        // Wave display
        waveText = new Text(WINDOW_WIDTH - 150, 30, "Wave: 1");
        waveText.setFill(Color.WHITE);
        waveText.setFont(Font.font("Arial Bold", 20));
        waveText.setEffect(dropShadow);

        setupGameOverUI();

        uiLayer.getChildren().addAll(scoreText, livesText, waveText);
    }

    private void setupGameOverUI() {
        gameOverGroup = new Group();

        Text gameOverText = new Text("GAME OVER");
        gameOverText.setFont(Font.font("Arial Bold", 48));
        gameOverText.setFill(Color.RED);
        gameOverText.setStroke(Color.WHITE);
        gameOverText.setStrokeWidth(2);
        gameOverText.setX(WINDOW_WIDTH/2 - 140);
        gameOverText.setY(WINDOW_HEIGHT/2);

        Glow glow = new Glow();
        glow.setLevel(0.8);
        gameOverText.setEffect(glow);

        Text restartText = new Text("Press SPACE to restart\nPress ESC to quit");
        restartText.setFont(Font.font("Arial", 24));
        restartText.setFill(Color.WHITE);
        restartText.setTextAlignment(TextAlignment.CENTER);
        restartText.setX(WINDOW_WIDTH/2 - 100);
        restartText.setY(WINDOW_HEIGHT/2 + 50);

        gameOverGroup.getChildren().addAll(gameOverText, restartText);
        gameOverGroup.setVisible(false);

        uiLayer.getChildren().add(gameOverGroup);
    }

    private void setupStartMenu() {
        startMenuGroup = new Group();

        // Title
        Text titleText = new Text("ASTEROID");
        titleText.setFont(Font.font("Arial Bold", 64));
        titleText.setFill(Color.WHITE);
        titleText.setStroke(Color.BLUE);
        titleText.setStrokeWidth(2);
        titleText.setX(WINDOW_WIDTH/2 - 150);
        titleText.setY(WINDOW_HEIGHT/3);

        // Controls info
        Text controlsText = new Text(
                "Controls:\n" +
                        "W,A,S,D - Move\n" +
                        "Q,E - Rotate\n" +
                        "SPACE - Shoot\n" +
                        "ESC - Pause"
        );

        controlsText.setFont(Font.font("Arial", 20));
        controlsText.setFill(Color.WHITE);
        controlsText.setTextAlignment(TextAlignment.CENTER);
        controlsText.setX(WINDOW_WIDTH/2 - 80);
        controlsText.setY(WINDOW_HEIGHT/2);

        // Create and position start button
        startButton = createStyledButton("Click to Start");
        startButton.setLayoutX(WINDOW_WIDTH/2 - 100);
        startButton.setLayoutY(WINDOW_HEIGHT * 0.75);

        Glow glow = new Glow();
        glow.setLevel(0.8);
        titleText.setEffect(glow);

        startMenuGroup.getChildren().addAll(titleText, controlsText, startButton);
        uiLayer.getChildren().add(startMenuGroup);
    }

    private void setupPauseMenu() {
        pauseMenuGroup = new Group();

        Text pauseText = new Text("PAUSED");
        pauseText.setFont(Font.font("Arial Bold", 48));
        pauseText.setFill(Color.WHITE);
        pauseText.setStroke(Color.BLUE);
        pauseText.setStrokeWidth(2);
        pauseText.setX(WINDOW_WIDTH/2 - 100);
        pauseText.setY(WINDOW_HEIGHT/2);

        Button resumeButton = createStyledButton("Resume");
        resumeButton.setLayoutX(WINDOW_WIDTH/2 - 100);
        resumeButton.setLayoutY(WINDOW_HEIGHT/2 + 50);

        restartButton = createStyledButton("Restart");
        restartButton.setLayoutX(WINDOW_WIDTH/2 - 100);
        restartButton.setLayoutY(WINDOW_HEIGHT/2 + 120);

        pauseMenuGroup.getChildren().addAll(pauseText, resumeButton, restartButton);
        pauseMenuGroup.setVisible(false);
        uiLayer.getChildren().add(pauseMenuGroup);
    }

    private void showBossWarning() {
        Text warningText = new Text("WARNING!\nBOSS APPROACHING");
        warningText.setFont(Font.font("Arial Bold", 48));
        warningText.setFill(Color.RED);
        warningText.setTextAlignment(TextAlignment.CENTER);
        warningText.setX(WINDOW_WIDTH/2 - 200);
        warningText.setY(WINDOW_HEIGHT/2);

        effectLayer.getChildren().add(warningText);

        // Create blinking effect
        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), warningText);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setCycleCount(6);
        fade.setOnFinished(e -> effectLayer.getChildren().remove(warningText));
        fade.play();
    }

    public Button getStartButton() {
        return startButton;
    }

    public Button getRestartButton() {
        return restartButton;
    }

    public void addGameObject(Character character) {
        gameLayer.getChildren().add(character.getSprite());

        // Special effect for boss appearance
        if (character instanceof Boss) {
            showBossWarning();
        }

        logger.debug("Added game object to stage: {}", character.getClass().getSimpleName());
    }

    public void removeGameObject(Character character) {
        gameLayer.getChildren().remove(character.getSprite());
        logger.debug("Removed game object from stage: {}", character.getClass().getSimpleName());
    }

    public void addBullet(Bullet bullet) {
        gameLayer.getChildren().add(bullet.getSprite());
        logger.debug("Added bullet to stage");
    }

    public void removeBullet(Bullet bullet) {
        gameLayer.getChildren().remove(bullet.getSprite());
        logger.debug("Removed bullet from stage");
    }

    public void showExplosion(Point2D position) {
        try {
            Image explosionImage = new Image(getClass().getResourceAsStream("/se233/asteroid/assets/Effects/explosion.png"));
            ImageView explosionView = new ImageView(explosionImage);

            explosionView.setX(position.getX() - explosionImage.getWidth()/2);
            explosionView.setY(position.getY() - explosionImage.getHeight()/2);

            effectLayer.getChildren().add(explosionView);

            // Create combined animation
            FadeTransition fade = new FadeTransition(Duration.seconds(0.5), explosionView);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);

            ScaleTransition scale = new ScaleTransition(Duration.seconds(0.5), explosionView);
            scale.setFromX(0.5);
            scale.setFromY(0.5);
            scale.setToX(1.5);
            scale.setToY(1.5);

            RotateTransition rotate = new RotateTransition(Duration.seconds(0.5), explosionView);
            rotate.setByAngle(90);

            ParallelTransition transition = new ParallelTransition(explosionView, fade, scale, rotate);
            transition.setOnFinished(e -> effectLayer.getChildren().remove(explosionView));
            transition.play();

        } catch (Exception e) {
            logger.error("Failed to show explosion effect", e);
        }
    }

    public void updateScore(int score) {
        scoreText.setText("Score: " + score);

        // Score popup effect
        Text scorePopup = new Text("+" + score);
        scorePopup.setFont(Font.font("Arial Bold", 24));
        scorePopup.setFill(Color.YELLOW);
        scorePopup.setX(scoreText.getX() + 100);
        scorePopup.setY(scoreText.getY());

        effectLayer.getChildren().add(scorePopup);

        FadeTransition fade = new FadeTransition(Duration.seconds(1), scorePopup);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(1), scorePopup);
        scale.setFromY(1.0);
        scale.setToY(1.5);

        ParallelTransition transition = new ParallelTransition(scorePopup, fade, scale);
        transition.setOnFinished(e -> effectLayer.getChildren().remove(scorePopup));
        transition.play();
    }

    public void updateLives(int lives) {
        livesText.setText("Lives: " + lives);

        // Flash effect when lives change
        FadeTransition fade = new FadeTransition(Duration.seconds(0.2), livesText);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        fade.setCycleCount(3);
        fade.setAutoReverse(true);
        fade.play();
    }

    public void updateWave(int wave) {
        waveText.setText("Wave: " + wave);

        // Wave announcement
        Text waveAnnounce = new Text("Wave " + wave);
        waveAnnounce.setFont(Font.font("Arial Bold", 48));
        waveAnnounce.setFill(Color.WHITE);
        waveAnnounce.setStroke(Color.BLUE);
        waveAnnounce.setStrokeWidth(2);
        waveAnnounce.setX(WINDOW_WIDTH/2 - 100);
        waveAnnounce.setY(WINDOW_HEIGHT/2);

        effectLayer.getChildren().add(waveAnnounce);

        // Combined animation
        FadeTransition fade = new FadeTransition(Duration.seconds(2), waveAnnounce);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(2), waveAnnounce);
        scale.setFromX(0.5);
        scale.setToX(1.5);

        ParallelTransition transition = new ParallelTransition(waveAnnounce, fade, scale);
        transition.setOnFinished(e -> effectLayer.getChildren().remove(waveAnnounce));
        transition.play();
    }

    public void showGameOver(int finalScore) {
        gameOverGroup.getChildren().clear();  // เคลียร์เนื้อหาเก่า

        Text gameOverText = new Text("GAME OVER");
        gameOverText.setFont(Font.font("Arial Bold", 48));
        gameOverText.setFill(Color.RED);
        gameOverText.setStroke(Color.WHITE);
        gameOverText.setStrokeWidth(2);
        gameOverText.setX(WINDOW_WIDTH/2 - 140);
        gameOverText.setY(WINDOW_HEIGHT/2 - 50);

        Text finalScoreText = new Text("Final Score: " + finalScore);
        finalScoreText.setFont(Font.font("Arial Bold", 36));
        finalScoreText.setFill(Color.WHITE);
        finalScoreText.setX(WINDOW_WIDTH/2 - 100);
        finalScoreText.setY(WINDOW_HEIGHT/2 + 20);

        Button restartButton = createStyledButton("Click to Restart");
        restartButton.setLayoutX(WINDOW_WIDTH/2 - 100);
        restartButton.setLayoutY(WINDOW_HEIGHT/2 + 60);

        // เพิ่ม Glow effect
        Glow glow = new Glow();
        glow.setLevel(0.8);
        gameOverText.setEffect(glow);

        // เพิ่ม animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), gameOverGroup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // เพิ่มทุกอย่างเข้าไปใน gameOverGroup
        gameOverGroup.getChildren().addAll(gameOverText, finalScoreText, restartButton);
        gameOverGroup.setVisible(true);

        fadeIn.play();
    }

    public void hideStartMenu() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), startMenuGroup);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> startMenuGroup.setVisible(false));
        fadeOut.play();
    }

    public void showPauseMenu() {
        pauseMenuGroup.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), pauseMenuGroup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // เพิ่ม blur effect ให้กับ background
        GaussianBlur blur = new GaussianBlur(5);
        gameLayer.setEffect(blur);
    }

    public void hidePauseMenu() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), pauseMenuGroup);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> pauseMenuGroup.setVisible(false));
        fadeOut.play();

        // ลบ blur effect
        gameLayer.setEffect(null);
    }

    public void hideGameOver() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), gameOverGroup);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> gameOverGroup.setVisible(false));
        fadeOut.play();
    }

    public void showVictory(int score) {
        Group victoryGroup = new Group();

        // Create Victory text
        Text victoryText = new Text("VICTORY!");
        victoryText.setFont(Font.font("Arial Bold", 64));
        victoryText.setFill(Color.GOLD);
        victoryText.setStroke(Color.WHITE);
        victoryText.setStrokeWidth(3);
        victoryText.setX(WINDOW_WIDTH/2 - 150);
        victoryText.setY(WINDOW_HEIGHT/2 - 50);

        // Create congratulatory message
        Text congratsText = new Text("Congratulations! You defeated the boss!");
        congratsText.setFont(Font.font("Arial Bold", 24));
        congratsText.setFill(Color.WHITE);
        congratsText.setX(WINDOW_WIDTH/2 - 200);
        congratsText.setY(WINDOW_HEIGHT/2 + 10);

        // Create score text
        Text finalScoreText = new Text("Final Score: " + score);
        finalScoreText.setFont(Font.font("Arial Bold", 36));
        finalScoreText.setFill(Color.WHITE);
        finalScoreText.setX(WINDOW_WIDTH/2 - 100);
        finalScoreText.setY(WINDOW_HEIGHT/2 + 60);

        // Create restart button
        Button restartButton = createStyledButton("Play Again");
        restartButton.setLayoutX(WINDOW_WIDTH/2 - 100);
        restartButton.setLayoutY(WINDOW_HEIGHT/2 + 100);

        // Add special effects
        Glow glow = new Glow();
        glow.setLevel(1.0);
        victoryText.setEffect(glow);

        // Create star particle effects
        for (int i = 0; i < 5; i++) {
            Text star = new Text("★");
            star.setFont(Font.font("Arial", 48));
            star.setFill(Color.YELLOW);
            star.setX(WINDOW_WIDTH/2 - 200 + (i * 100));
            star.setY(WINDOW_HEIGHT/2 - 100);

            // Add rotation animation to stars
            RotateTransition rotate = new RotateTransition(Duration.seconds(2), star);
            rotate.setByAngle(360);
            rotate.setCycleCount(RotateTransition.INDEFINITE);
            rotate.play();

            victoryGroup.getChildren().add(star);
        }

        // Add all elements to victory group
        victoryGroup.getChildren().addAll(victoryText, congratsText, finalScoreText, restartButton);

        // Add to UI layer
        uiLayer.getChildren().add(victoryGroup);

        // Create fade-in animation for the victory screen
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), victoryGroup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // Add blur effect to game layer
        GaussianBlur blur = new GaussianBlur(5);
        gameLayer.setEffect(blur);

        // Create scale animation for victory text
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.5), victoryText);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.0);
        scale.setToY(1.0);

        // Play animations together
        ParallelTransition transition = new ParallelTransition(fadeIn, scale);
        transition.play();

        // Log the victory
        logger.info("Victory screen shown with score: {}", score);
    }
}


