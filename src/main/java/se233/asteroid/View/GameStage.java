package se233.asteroid.View;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.util.Duration;
import se233.asteroid.model.Character;
import se233.asteroid.model.Bullet;
import se233.asteroid.model.Boss;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameStage extends Pane {
    private static final Logger logger = LogManager.getLogger(GameStage.class);

    private ImageView backgroundView;
    private ImageView starsView;
    private Pane gameLayer;        // For game objects (ships, asteroids, bullets)
    private Pane effectLayer;      // For visual effects
    private Pane uiLayer;          // For UI elements
    private Text scoreText;
    private Text livesText;
    private Text waveText;
    private Group gameOverGroup;
    private Group startMenuGroup;
    private Group pauseMenuGroup;
    private boolean isGameStarted;
    private boolean isPaused;

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

    private void setupBackground() {
        try {
            // Load and set up parallax background layers
            Image backgroundImage = new Image(getClass().getResourceAsStream("/se233/asteroid/assets/Backgrounds/space_background.png"));
            Image starsImage = new Image(getClass().getResourceAsStream("/se233/asteroid/assets/Backgrounds/stars.png"));

            backgroundView = new ImageView(backgroundImage);
            starsView = new ImageView(starsImage);

            // Make background images resize with the window
            backgroundView.fitWidthProperty().bind(this.widthProperty());
            backgroundView.fitHeightProperty().bind(this.heightProperty());
            starsView.fitWidthProperty().bind(this.widthProperty());
            starsView.fitHeightProperty().bind(this.heightProperty());

            // Add parallax effect
            starsView.setOpacity(0.7);

            getChildren().addAll(backgroundView, starsView);

        } catch (Exception e) {
            logger.error("Failed to load background images", e);
            setStyle("-fx-background-color: black;");
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
        uiLayer.setMouseTransparent(true);

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
                        "WASD - Move\n" +
                        "Q/E - Rotate\n" +
                        "SPACE - Shoot\n" +
                        "ESC - Pause"
        );
        controlsText.setFont(Font.font("Arial", 20));
        controlsText.setFill(Color.WHITE);
        controlsText.setTextAlignment(TextAlignment.CENTER);
        controlsText.setX(WINDOW_WIDTH/2 - 80);
        controlsText.setY(WINDOW_HEIGHT/2);

        Text startText = new Text("Press SPACE to start");
        startText.setFont(Font.font("Arial", 24));
        startText.setFill(Color.WHITE);
        startText.setX(WINDOW_WIDTH/2 - 100);
        startText.setY(WINDOW_HEIGHT * 0.75);

        Glow glow = new Glow();
        glow.setLevel(0.8);
        titleText.setEffect(glow);

        startMenuGroup.getChildren().addAll(titleText, controlsText, startText);
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

        Text resumeText = new Text("Press ESC to resume\nPress R to restart");
        resumeText.setFont(Font.font("Arial", 24));
        resumeText.setFill(Color.WHITE);
        resumeText.setTextAlignment(TextAlignment.CENTER);
        resumeText.setX(WINDOW_WIDTH/2 - 100);
        resumeText.setY(WINDOW_HEIGHT/2 + 50);

        pauseMenuGroup.getChildren().addAll(pauseText, resumeText);
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
        Text finalScoreText = new Text("Final Score");}}