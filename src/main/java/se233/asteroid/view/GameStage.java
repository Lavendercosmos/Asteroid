package se233.asteroid.view;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import se233.asteroid.model.Character;
import se233.asteroid.model.Bullet;
import se233.asteroid.model.Boss;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class GameStage extends Pane {
    private static final Logger logger = LogManager.getLogger(GameStage.class);

    // Constants
    public static double WINDOW_WIDTH = 800;
    public static double WINDOW_HEIGHT = 625;
    private static final String STYLE_HEADER = "-fx-font-family: 'Press Start 2P', Arial; -fx-font-size: 48px; -fx-fill: white; -fx-stroke: #4a90e2; -fx-stroke-width: 2px;";
    private static final String STYLE_NORMAL = "-fx-font-family: Arial; -fx-font-size: 24px; -fx-fill: white;";
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;

    // Layers
    private ImageView backgroundView;
    private Pane gameLayer;
    private Pane effectLayer;
    private Pane uiLayer;
    private Pane particleLayer;

    // UI Elements
    public Text scoreText;
    private Text livesText;
    private Text waveText;
    private Group gameOverGroup;
    private Group startMenuGroup;
    private Group pauseMenuGroup;
    private Button startButton;
    private Button restartButton;
    private Button resumeButton;
    private VBox startMenuVBox;

    // Game State
    private boolean isGameStarted;
    private boolean isPaused;
    private int currentWave;
    private int currentScore;
    private int currentLives;

    // Scaling
    private Scale scale;
    private double scaleX = 1;
    private double scaleY = 1;

    private Group victoryGroup;


    public GameStage() {
        initializeStage();
        setupLayers();
        setupBackground();
        setupUI();
        setupGameState();
        setupScaling();
        logger.info("GameStage initialized successfully");
    }

    private void initializeStage() {
        setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setStyle("-fx-background-color: black;");
        currentWave = 1;
        currentScore = 0;
        currentLives = 3;
    }

    private void setupLayers() {
        gameLayer = new Pane();
        effectLayer = new Pane();
        uiLayer = new Pane();
        particleLayer = new Pane();

        gameLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        effectLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        uiLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        particleLayer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        effectLayer.setMouseTransparent(true);
        particleLayer.setMouseTransparent(true);

        getChildren().addAll(gameLayer, particleLayer, effectLayer, uiLayer);
        logger.debug("Layers setup completed");
    }

    private void setupScaling() {
        scale = new Scale(1, 1);
        getTransforms().add(scale);

        // Add listener for parent changes
        parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                // Bind prefSize to parent once we have a parent
                prefWidthProperty().bind(newParent.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
                prefHeightProperty().bind(newParent.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
            } else {
                // Unbind if parent is removed
                prefWidthProperty().unbind();
                prefHeightProperty().unbind();
                // Reset to default size
                setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            }
        });

        // Set initial size
        setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        logger.debug("Scaling setup completed with initial size: {}x{}", WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private void setupBackground() {
        try {
            Image backgroundImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/se233/asteroid/assets/Backgrounds/SpaceBG.gif")
            ));

            if (backgroundImage.isError()) {
                logger.error("Failed to load background image");
                createGradientBackground();
                return;
            }

            backgroundView = new ImageView(backgroundImage);
            backgroundView.setFitWidth(WINDOW_WIDTH);
            backgroundView.setFitHeight(WINDOW_HEIGHT);
            backgroundView.setPreserveRatio(false);

            gameLayer.getChildren().add(backgroundView);
            createParallaxStars();

            logger.info("Background setup completed");
        } catch (Exception e) {
            logger.error("Failed to setup background", e);
            createGradientBackground();
        }
    }

    private void createGradientBackground() {
        gameLayer.setStyle("-fx-background-color: linear-gradient(to bottom, #000022, #000066);");
        createParallaxStars();
        logger.info("Created gradient background with stars as fallback");
    }

    private void createParallaxStars() {
        for (int layer = 0; layer < 3; layer++) {
            final int starCount = 20 - (layer * 5);
            final double speed = (layer + 1) * 2;
            createStarLayer(starCount, speed, layer);
        }
    }

    private void createStarLayer(int count, double speed, int layer) {
        for (int i = 0; i < count; i++) {
            Text star = new Text("✦");
            star.setFill(Color.WHITE);
            star.setOpacity(0.5 + (layer * 0.2));
            star.setFont(Font.font("Arial", 8 + (layer * 2)));

            // Random initial position
            star.setX(Math.random() * WINDOW_WIDTH);
            star.setY(Math.random() * WINDOW_HEIGHT);

            TranslateTransition move = new TranslateTransition(Duration.seconds(speed), star);
            move.setFromY(-10);
            move.setToY(WINDOW_HEIGHT + 10);
            move.setCycleCount(TranslateTransition.INDEFINITE);
            move.setInterpolator(Interpolator.LINEAR);

            // Store animation reference for later adjustment
            star.getProperties().put("animation", move);

            particleLayer.getChildren().add(star);
            move.play();
        }
    }

    public void setupUI() {
        setupHUD();
        setupStartMenu();
        setupPauseMenu();
        setupGameOverScreen();
        setupVictoryScreen();
        setupBossHealthBar(); // Add this line
        logger.debug("UI setup completed");
    }

    private void setupHUD() {
        // สร้าง score text ด้วยสไตล์ที่ชัดเจน
        scoreText = new Text(20, 30, "Score: "+ currentScore);
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        scoreText.setFill(Color.YELLOW); // เปลี่ยนสีให้เด่นชัด
        scoreText.setEffect(new DropShadow(3, Color.BLACK)); // เพิ่ม effect ให้อ่านง่าย

        // จัดการ UI อื่นๆ
        livesText = createStyledText("Lives: " + currentLives, 20, 60);
        waveText = createStyledText("Wave: " + currentWave, WINDOW_WIDTH - 150, 30);

        uiLayer.getChildren().addAll(scoreText, livesText, waveText);

        // นำ score text ไว้ด้านหน้าสุด
        scoreText.toFront();
    }


    private Text createStyledText(String content, double x, double y) {
        Text text = new Text(x, y, content);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        text.setFill(Color.WHITE);
        text.setEffect(new DropShadow(2, Color.BLUE));
        return text;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 5;" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "-fx-background-color: #45a049;");
            button.setEffect(new Glow(0.4));
        });

        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle() + "-fx-background-color: #4CAF50;");
            button.setEffect(null);
        });

        return button;
    }

    private void setupStartMenu() {
        startMenuGroup = new Group();
        startMenuVBox = new VBox(20);
        startMenuVBox.setAlignment(Pos.CENTER);

        Text titleText = new Text("ASTEROID");
        titleText.setStyle(STYLE_HEADER);

        Text controlsText = new Text(
                "Controls:\n" +
                        "W,A,S,D - Move\n" +
                        "Q,E - Rotage\n" +
                        "Space - Shoot\n" +
                        "ESC - Pause"
        );
        controlsText.setStyle(STYLE_NORMAL);
        controlsText.setTextAlignment(TextAlignment.CENTER);

        startButton = createStyledButton("Start Game");

        startMenuVBox.getChildren().addAll(titleText, controlsText, startButton);
        startMenuVBox.setLayoutX((WINDOW_WIDTH - 300) / 2);
        startMenuVBox.setLayoutY((WINDOW_HEIGHT - 400) / 2);

        startMenuGroup.getChildren().add(startMenuVBox);
        uiLayer.getChildren().add(startMenuGroup);
    }

    private void setupPauseMenu() {
        pauseMenuGroup = new Group();
        VBox pauseMenuVBox = new VBox(20);
        pauseMenuVBox.setAlignment(Pos.CENTER);

        Text pauseText = new Text("PAUSED");
        pauseText.setStyle(STYLE_HEADER);

        resumeButton = createStyledButton("Resume");
        restartButton = createStyledButton("Restart");

        pauseMenuVBox.getChildren().addAll(pauseText, resumeButton, restartButton);
        pauseMenuVBox.setLayoutX((WINDOW_WIDTH - 300) / 2);
        pauseMenuVBox.setLayoutY((WINDOW_HEIGHT - 300) / 2);

        pauseMenuGroup.getChildren().add(pauseMenuVBox);
        pauseMenuGroup.setVisible(false);
        uiLayer.getChildren().add(pauseMenuGroup);
    }

    private void setupGameOverScreen() {
        gameOverGroup = new Group();
        VBox gameOverVBox = new VBox(20);
        gameOverVBox.setAlignment(Pos.CENTER);

        Text gameOverText = new Text("GAME OVER");
        gameOverText.setStyle(STYLE_HEADER);

        Button restartButton = createStyledButton("Try Again");

        gameOverVBox.getChildren().addAll(gameOverText, restartButton);
        gameOverVBox.setLayoutX((WINDOW_WIDTH - 300) / 2);
        gameOverVBox.setLayoutY((WINDOW_HEIGHT - 200) / 2);

        gameOverGroup.getChildren().add(gameOverVBox);
        gameOverGroup.setVisible(false);
        uiLayer.getChildren().add(gameOverGroup);
    }

    private void setupVictoryScreen() {
        // Similar to gameOverScreen but with victory message
        Group victoryGroup = new Group();
        VBox victoryVBox = new VBox(20);
        victoryVBox.setAlignment(Pos.CENTER);

        Text victoryText = new Text("VICTORY!");
        victoryText.setStyle(STYLE_HEADER);
        victoryText.setFill(Color.GOLD);

        Button playAgainButton = createStyledButton("Play Again");

        victoryVBox.getChildren().addAll(victoryText, playAgainButton);
        victoryVBox.setLayoutX((WINDOW_WIDTH - 300) / 2);
        victoryVBox.setLayoutY((WINDOW_HEIGHT - 200) / 2);

        victoryGroup.getChildren().add(victoryVBox);
        victoryGroup.setVisible(false);
        uiLayer.getChildren().add(victoryGroup);
    }

    private void setupGameState() {
        isGameStarted = false;
        isPaused = false;
    }

    public void handleResize(double width, double height) {
        scaleX = width / WINDOW_WIDTH;
        scaleY = height / WINDOW_HEIGHT;

        // Keep scale within bounds
        double finalScale = Math.min(Math.max(Math.min(scaleX, scaleY), MIN_SCALE), MAX_SCALE);

        scale.setX(finalScale);
        scale.setY(finalScale);

        // Center the game
        setTranslateX((width - WINDOW_WIDTH * finalScale) / 2);
        setTranslateY((height - WINDOW_HEIGHT * finalScale) / 2);

        // Update UI elements
        updateUIElements();
        updateBackgroundEffects();

        logger.debug("Game stage resized - Scale: {}", finalScale);
    }

    private void updateUIElements() {
        double finalScale = Math.min(scaleX, scaleY);

        // Update HUD
        scoreText.setFont(Font.font("Arial", FontWeight.BOLD, 20 * finalScale));
        livesText.setFont(Font.font("Arial", FontWeight.BOLD, 20 * finalScale));
        waveText.setFont(Font.font("Arial", FontWeight.BOLD, 20 * finalScale));

        // Update menus
        updateMenuPositions();
    }

    private void updateBackgroundEffects() {
        // Update parallax star animations based on current scale
        particleLayer.getChildren().forEach(node -> {
            if (node.getProperties().containsKey("animation")) {
                TranslateTransition tt = (TranslateTransition) node.getProperties().get("animation");
                tt.stop();

                // Adjust star motion speed based on current scale
                double baseSpeed = tt.getDuration().toSeconds();
                double scaledSpeed = baseSpeed / scale.getY(); // Faster at higher zoom levels
                tt.setDuration(Duration.seconds(scaledSpeed));

                // Adjust star path based on current height
                tt.setFromY(-10 * scale.getY());
                tt.setToY(WINDOW_HEIGHT * scale.getY() + 10);

                tt.play();
            }
        });

        // Update background image size if it exists
        if (backgroundView != null) {
            backgroundView.setFitWidth(WINDOW_WIDTH * scale.getX());
            backgroundView.setFitHeight(WINDOW_HEIGHT * scale.getY());
        }

        logger.debug("Background effects updated with scale: ({}, {})", scale.getX(), scale.getY());
    }

    private void updateMenuPositions() {
        if (startMenuVBox != null) {
            startMenuVBox.setLayoutX((getWidth() - 300 * scaleX) / 2);
            startMenuVBox.setLayoutY((getHeight() - 400 * scaleY) / 2);
        }

        // Update other menu positions similarly
    }

    private void updateScale() {
        // Calculate scale based on current window dimensions
        double parentWidth = getParent() != null ? getParent().getLayoutBounds().getWidth() : WINDOW_WIDTH;
        double parentHeight = getParent() != null ? getParent().getLayoutBounds().getHeight() : WINDOW_HEIGHT;

        scaleX = parentWidth / WINDOW_WIDTH;
        scaleY = parentHeight / WINDOW_HEIGHT;

        // Keep scale within bounds
        double finalScale = Math.min(Math.max(Math.min(scaleX, scaleY), MIN_SCALE), MAX_SCALE);

        // Update the scale transform
        scale.setX(finalScale);
        scale.setY(finalScale);

        // Center the game stage
        setTranslateX((parentWidth - WINDOW_WIDTH * finalScale) / 2);
        setTranslateY((parentHeight - WINDOW_HEIGHT * finalScale) / 2);

        // Update UI elements with new scale
        updateUIElements();
        updateBackgroundEffects();

        logger.debug("Scale updated - new scale: {}", finalScale);
    }

    // Coordinate conversion methods
    public Point2D screenToGame(Point2D screenPoint) {
        return new Point2D(
                (screenPoint.getX() - getTranslateX()) / scale.getX(),
                (screenPoint.getY() - getTranslateY()) / scale.getY()
        );
    }

    public Point2D gameToScreen(Point2D gamePoint) {
        return new Point2D(
                gamePoint.getX() * scale.getX() + getTranslateX(),
                gamePoint.getY() * scale.getY() + getTranslateY()
        );
    }

    // Game object management
    public void addGameObject(Character character) {
        gameLayer.getChildren().add(character.getSprite());
        if (character instanceof Boss) {
            showBossWarning();
        }
        logger.debug("Added game object: {}", character.getClass().getSimpleName());
    }

    public void removeGameObject(Character character) {
        gameLayer.getChildren().remove(character.getSprite());
        logger.debug("Removed game object: {}", character.getClass().getSimpleName());
    }

    public void addBullet(Bullet bullet) {
        gameLayer.getChildren().add(bullet.getSprite());
    }

    public void removeBullet(Bullet bullet) {
        gameLayer.getChildren().remove(bullet.getSprite());
    }
    private void showBossWarning() {
        Text warningText = new Text("WARNING!\nBOSS APPROACHING");
        warningText.setStyle(STYLE_HEADER);
        warningText.setFill(Color.RED);
        warningText.setTextAlignment(TextAlignment.CENTER);

        // Scale and position warning
        Point2D warningPos = gameToScreen(new Point2D(
                (WINDOW_WIDTH - 400) / 2,
                WINDOW_HEIGHT / 2
        ));
        warningText.setX(warningPos.getX());
        warningText.setY(warningPos.getY());
        warningText.setScaleX(scale.getX());
        warningText.setScaleY(scale.getY());

        effectLayer.getChildren().add(warningText);

        // Warning animation with scaled duration
        Timeline blink = new Timeline(
                new KeyFrame(Duration.seconds(0.5 / scale.getX()),
                        new KeyValue(warningText.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1.0 / scale.getX()),
                        new KeyValue(warningText.opacityProperty(), 0.0))
        );
        blink.setCycleCount(3);
        blink.setOnFinished(e -> effectLayer.getChildren().remove(warningText));
        blink.play();
    }

    public void showExplosion(Point2D position) {
        try {
            // Load explosion sprite sheet
            Image explosionImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/se233/asteroid/assets/PlayerShip/Explosion.png")
            ));

            ImageView explosionView = new ImageView(explosionImage);

            // Convert to screen coordinates and scale
            Point2D screenPos = gameToScreen(position);
            double scaledSize = 100 * scale.getX(); // Base size for explosion

            explosionView.setX(screenPos.getX() - scaledSize/2.2);
            explosionView.setY(screenPos.getY() - scaledSize/2);
            explosionView.setFitWidth(scaledSize);
            explosionView.setFitHeight(scaledSize);
            explosionView.setViewport(new Rectangle2D(0,0,30,30));

            effectLayer.getChildren().add(explosionView);

            // Create explosion animation timeline
            Timeline explosionAnimation = new Timeline();

            // 8 frames of animation
            for(int i = 0; i < 8; i++) {
                KeyFrame frame = new KeyFrame(
                        Duration.seconds(i * 0.05), // 50ms per frame
                        new KeyValue(explosionView.viewportProperty(),
                                new Rectangle2D(i * 100, 0, 30, 30))
                );
                explosionAnimation.getKeyFrames().add(frame);
            }

            // Add scaling and fade effects
            ParallelTransition animation = new ParallelTransition(
                    explosionAnimation,
                    createScaleTransition(explosionView, 0.5, 1.5, 0.4),
                    createFadeTransition(explosionView, 1.0, 0.0, 0.4)
            );

            animation.setOnFinished(e -> effectLayer.getChildren().remove(explosionView));
            animation.play();

        } catch (Exception e) {
            logger.error("Failed to show explosion effect", e);
            // Fallback to simple explosion effect
            createSimpleExplosionEffect(position);
        }
    }

    private void createSimpleExplosionEffect(Point2D position) {
        Circle explosionCircle = new Circle();
        Point2D screenPos = gameToScreen(position);

        explosionCircle.setCenterX(screenPos.getX());
        explosionCircle.setCenterY(screenPos.getY());
        explosionCircle.setRadius(20 * scale.getX());
        explosionCircle.setFill(Color.ORANGE);

        effectLayer.getChildren().add(explosionCircle);

        ParallelTransition animation = new ParallelTransition(
                createFadeTransition(explosionCircle, 1.0, 0.0, 0.4),
                createScaleTransition(explosionCircle, 0.5, 1.5, 0.4)
        );

        animation.setOnFinished(e -> effectLayer.getChildren().remove(explosionCircle));
        animation.play();
    }

    // แก้ไขเมธอด updateScore
    public void updateScore(int score) {
        this.currentScore = score;
        if (scoreText != null) {
            Platform.runLater(() -> {
                scoreText.setText("Score: " + score);

                // เพิ่ม effect เมื่อคะแนนเปลี่ยน
                ScaleTransition st = new ScaleTransition(Duration.millis(100), scoreText);
                st.setFromX(1.2);
                st.setFromY(1.2);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });
        }
        logger.debug("Score updated to: {}", score);
    }

    // เพิ่มเมธอดสำหรับรีเซ็ตคะแนน
    private void resetScore() {
        currentScore = 0;
        if (scoreText != null) {
            scoreText.setText("Score:0");
        }
    }

    public void showScorePopup(int points, Point2D position) {
        Text scorePopup = new Text("+" + points);
        scorePopup.setFont(Font.font("Arial", FontWeight.BOLD, 24 * scale.getX()));
        scorePopup.setFill(Color.YELLOW);

        // Convert game position to screen position
        Point2D screenPos = gameToScreen(position);
        scorePopup.setX(screenPos.getX());
        scorePopup.setY(screenPos.getY());

        // Add to effect layer
        effectLayer.getChildren().add(scorePopup);

        double animationSpeed = 0.5 / Math.max(scale.getX(), scale.getY());
        ParallelTransition animation = new ParallelTransition(
                // Fade out
                createFadeTransition(scorePopup, 1.0, 0.0, animationSpeed),
                // Float up
                createMoveTransition(scorePopup,
                        scorePopup.getX(),
                        scorePopup.getY() - 50,
                        animationSpeed)
        );

        animation.setOnFinished(e -> effectLayer.getChildren().remove(scorePopup));
        animation.play();
    }



    public void updateLives(int lives) {
        this.currentLives = lives;
        livesText.setText("Lives: " + lives);

        // Flash effect with scaled duration
        double flashSpeed = 0.2 / scale.getX();
        FadeTransition fade = new FadeTransition(Duration.seconds(flashSpeed), livesText);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        fade.setCycleCount(3);
        fade.setAutoReverse(true);
        fade.play();
    }

    public void hideGameOver() {
        // Reset the game over display
        double fadeSpeed = 0.5 / scale.getX();
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(fadeSpeed), gameOverGroup);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            gameOverGroup.setVisible(false);
            gameLayer.setEffect(null);

            // Remove the score text that was added in showGameOver
            VBox gameOverBox = (VBox)gameOverGroup.getChildren().get(0);
            if (gameOverBox.getChildren().size() > 2) {  // If there's a score text
                gameOverBox.getChildren().remove(1);  // Remove the score text
            }
        });
        fadeOut.play();

        logger.debug("Game over screen hidden");
    }

    public void showVictory(int finalScore) {
        victoryGroup.setVisible(true);
        gameLayer.setEffect(new GaussianBlur(5 * scale.getX()));

        // Add final score to victory screen
        Text scoreText = new Text("Final Score: " + finalScore);
        scoreText.setStyle(STYLE_NORMAL);
        scoreText.setFill(Color.GOLD);
        scoreText.setScaleX(scale.getX());
        scoreText.setScaleY(scale.getY());

        VBox victoryBox = (VBox)victoryGroup.getChildren().get(0);
        if (victoryBox.getChildren().size() <= 2) {  // Only add score if not already present
            victoryBox.getChildren().add(1, scoreText);
        }

        // Victory animation with scaled duration
        double fadeSpeed = 0.5 / scale.getX();
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(fadeSpeed), victoryGroup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Add victory particles effect
        showVictoryParticles();

        logger.info("Victory screen shown with score: {}", finalScore);
    }

    public void hideVictory() {
        double fadeSpeed = 0.5 / scale.getX();
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(fadeSpeed), victoryGroup);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            victoryGroup.setVisible(false);
            gameLayer.setEffect(null);

            // Remove the score text
            VBox victoryBox = (VBox)victoryGroup.getChildren().get(0);
            if (victoryBox.getChildren().size() > 2) {  // If there's a score text
                victoryBox.getChildren().remove(1);  // Remove the score text
            }
        });
        fadeOut.play();

        logger.debug("Victory screen hidden");
    }

    private void showVictoryParticles() {
        // Create celebratory particle effects
        for (int i = 0; i < 20; i++) {
            createVictoryParticle();
        }
    }

    private void createVictoryParticle() {
        Text particle = new Text("★");
        particle.setFill(Color.GOLD);
        particle.setFont(Font.font("Arial", FontWeight.BOLD, 20 * scale.getX()));

        // Random starting position at bottom of screen
        double startX = Math.random() * WINDOW_WIDTH;
        particle.setTranslateX(startX);
        particle.setTranslateY(WINDOW_HEIGHT);

        particleLayer.getChildren().add(particle);

        // Create particle animation
        double duration = 1 + Math.random() * 2;  // Random duration between 1-3 seconds
        double targetX = startX + (Math.random() * 200 - 100);  // Random X movement

        ParallelTransition animation = new ParallelTransition(
                createMoveTransition(particle, targetX, -100, duration),  // Move up and sideways
                createFadeTransition(particle, 1.0, 0.0, duration),
                createRotateTransition(particle, 360 * (Math.random() > 0.5 ? 1 : -1), duration)
        );

        animation.setOnFinished(e -> particleLayer.getChildren().remove(particle));
        animation.play();
    }

    // Helper method สำหรับสร้าง animation การเคลื่อนที่
    private TranslateTransition createMoveTransition(Node node, double toX, double toY, double duration) {
        TranslateTransition move = new TranslateTransition(Duration.seconds(duration), node);
        move.setToX(toX);
        move.setToY(toY);
        return move;
    }

    public void updateWave(int wave) {
        this.currentWave = wave;
        waveText.setText("Wave: " + wave);
        showWaveAnnouncement(wave);
    }

    private void showWaveAnnouncement(int wave) {
        Text announcement = new Text("Wave " + wave);
        announcement.setStyle(STYLE_HEADER);

        Point2D announcementPos = gameToScreen(new Point2D(
                (WINDOW_WIDTH - 200) / 2,
                WINDOW_HEIGHT / 2
        ));
        announcement.setX(announcementPos.getX());
        announcement.setY(announcementPos.getY());
        announcement.setScaleX(scale.getX());
        announcement.setScaleY(scale.getY());

        effectLayer.getChildren().add(announcement);

        // Calculate animation speed based on scale
        double animationSpeed = 0.5 / Math.max(scale.getX(), scale.getY());

        // Create and play animation
        ParallelTransition animation = new ParallelTransition(
                createFadeTransition(announcement, 1.0, 0.0, animationSpeed),
                createScaleTransition(announcement, 0.5, 1.5, animationSpeed)
        );
        animation.setOnFinished(e -> effectLayer.getChildren().remove(announcement));
        animation.play();
    }

    // Menu control
    public void hideStartMenu() {
        double fadeSpeed = 0.5 / scale.getX();
        FadeTransition fade = new FadeTransition(Duration.seconds(fadeSpeed), startMenuGroup);
        fade.setToValue(0);
        fade.setOnFinished(e -> startMenuGroup.setVisible(false));
        fade.play();
    }

    public void showPauseMenu() {
        pauseMenuGroup.setVisible(true);
        gameLayer.setEffect(new GaussianBlur(5 * scale.getX()));

        double fadeSpeed = 0.3 / scale.getX();
        FadeTransition fade = new FadeTransition(Duration.seconds(fadeSpeed), pauseMenuGroup);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public void hidePauseMenu() {
        double fadeSpeed = 0.3 / scale.getX();
        FadeTransition fade = new FadeTransition(Duration.seconds(fadeSpeed), pauseMenuGroup);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            pauseMenuGroup.setVisible(false);
            gameLayer.setEffect(null);
        });
        fade.play();
    }

    public void showGameOver(int finalScore) {
        gameOverGroup.setVisible(true);
        gameLayer.setEffect(new GaussianBlur(5 * scale.getX()));

        Text scoreText = new Text("Final Score: " + finalScore);
        scoreText.setStyle(STYLE_NORMAL);
        scoreText.setScaleX(scale.getX());
        scoreText.setScaleY(scale.getY());

        VBox gameOverBox = (VBox)gameOverGroup.getChildren().get(0);
        gameOverBox.getChildren().add(1, scoreText);

        double fadeSpeed = 0.5 / scale.getX();
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(fadeSpeed), gameOverGroup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    // Animation helpers with scale-aware durations
    private FadeTransition createFadeTransition(Node node, double from, double to, double speed) {
        FadeTransition fade = new FadeTransition(Duration.seconds(speed), node);
        fade.setFromValue(from);
        fade.setToValue(to);
        return fade;
    }

    private ScaleTransition createScaleTransition(Node node, double from, double to, double speed) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(speed), node);
        scaleTransition.setFromX(from * scale.getX());
        scaleTransition.setFromY(from * scale.getY());
        scaleTransition.setToX(to * scale.getX());
        scaleTransition.setToY(to * scale.getY());
        return scaleTransition;
    }

    private RotateTransition createRotateTransition(Node node, double angle, double speed) {
        RotateTransition rotate = new RotateTransition(Duration.seconds(speed), node);
        rotate.setByAngle(angle);
        return rotate;
    }

    private void setupBossHealthBar() {
        // Create boss health bar container
        Group bossHealthGroup = new Group();

        // Background bar
        Rectangle healthBarBg = new Rectangle(400, 20);
        healthBarBg.setFill(Color.rgb(60, 60, 60, 0.8));
        healthBarBg.setStroke(Color.BLACK);
        healthBarBg.setStrokeWidth(2);
        healthBarBg.setArcWidth(10);
        healthBarBg.setArcHeight(10);

        // Health bar
        Rectangle healthBar = new Rectangle(400, 20);
        healthBar.setFill(Color.RED);
        healthBar.setArcWidth(10);
        healthBar.setArcHeight(10);

        // Enraged indicator
        Text enragedText = new Text("ENRAGED");
        enragedText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        enragedText.setFill(Color.RED);
        enragedText.setStroke(Color.BLACK);
        enragedText.setStrokeWidth(1);
        enragedText.setVisible(false);
        enragedText.setX(410);
        enragedText.setY(15);

        bossHealthGroup.getChildren().addAll(healthBarBg, healthBar, enragedText);
        bossHealthGroup.setTranslateX((WINDOW_WIDTH - 400) / 2);
        bossHealthGroup.setTranslateY(20);
        bossHealthGroup.setVisible(false);

        // Store references for updating
        bossHealthGroup.getProperties().put("healthBar", healthBar);
        bossHealthGroup.getProperties().put("enragedText", enragedText);

        uiLayer.getChildren().add(bossHealthGroup);
        bossHealthGroup.toFront();

        // Store reference to update later
        uiLayer.getProperties().put("bossHealthGroup", bossHealthGroup);
    }

    public void updateBossHealth(double healthPercentage, boolean isEnraged) {
        Group bossHealthGroup = (Group) uiLayer.getProperties().get("bossHealthGroup");
        if (bossHealthGroup != null) {
            Rectangle healthBar = (Rectangle) bossHealthGroup.getProperties().get("healthBar");
            Text enragedText = (Text) bossHealthGroup.getProperties().get("enragedText");

            // Show health bar if not visible
            if (!bossHealthGroup.isVisible()) {
                bossHealthGroup.setVisible(true);

                // Entrance animation
                bossHealthGroup.setScaleX(0);
                ScaleTransition st = new ScaleTransition(Duration.seconds(0.3), bossHealthGroup);
                st.setToX(1);
                st.play();
            }

            // Update health bar width with animation
            double targetWidth = 400 * healthPercentage;
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(0.2),
                            new KeyValue(healthBar.widthProperty(), targetWidth, Interpolator.EASE_OUT)
                    )
            );
            timeline.play();

            // Update color based on health percentage
            Color healthColor;
            if (healthPercentage > 0.6) {
                healthColor = Color.RED;
            } else if (healthPercentage > 0.3) {
                healthColor = Color.ORANGE;
            } else {
                healthColor = Color.rgb(255, 50, 50); // Bright red
            }
            healthBar.setFill(healthColor);

            // Update enraged status
            enragedText.setVisible(isEnraged);
            if (isEnraged) {
                // Flashing animation for enraged text
                Timeline flash = new Timeline(
                        new KeyFrame(Duration.seconds(0.5), new KeyValue(enragedText.opacityProperty(), 1.0)),
                        new KeyFrame(Duration.seconds(1.0), new KeyValue(enragedText.opacityProperty(), 0.3))
                );
                flash.setCycleCount(Timeline.INDEFINITE);
                flash.play();
            }
        }
    }

    public void hideBossHealth() {
        Group bossHealthGroup = (Group) uiLayer.getProperties().get("bossHealthGroup");
        if (bossHealthGroup != null && bossHealthGroup.isVisible()) {
            // Exit animation
            ScaleTransition st = new ScaleTransition(Duration.seconds(0.3), bossHealthGroup);
            st.setToX(0);
            st.setOnFinished(e -> bossHealthGroup.setVisible(false));
            st.play();
        }
    }

        // Getters and utility methods
        public Button getStartButton () {
            return startButton;
        }
        public Button getRestartButton () {
            return restartButton;
        }
        public Button getResumeButton () {
            return resumeButton;
        }
        public boolean isGameStarted () {
            return isGameStarted;
        }
        public boolean isPaused () {
            return isPaused;
        }

        public void reset () {
            // Reset game state
            isGameStarted = false;
            isPaused = false;
            currentWave = 1;
            currentLives = 3;
            resetScore();

            // Reset UI elements
            scoreText.setText("Score: 0");
            livesText.setText("Lives: 3");
            waveText.setText("Wave: 1");

            // Clear all layers
            gameLayer.getChildren().clear();
            effectLayer.getChildren().clear();
            particleLayer.getChildren().clear();

            // Reset background
            setupBackground();

            // Reset menus
            startMenuGroup.setVisible(true);
            startMenuGroup.setOpacity(1);
            pauseMenuGroup.setVisible(false);
            gameOverGroup.setVisible(false);

            // Clear effects
            gameLayer.setEffect(null);

            // Reset scale if needed
            updateScale();

            logger.info("Game stage reset completed");
        }

    public double getStageWidth() { return WINDOW_WIDTH * scale.getX(); }
    public double getStageHeight() { return WINDOW_HEIGHT * scale.getY(); }
    public double getCurrentScaleX() {return scale.getX();}
    public double getCurrentScaleY() { return scale.getY();}
}