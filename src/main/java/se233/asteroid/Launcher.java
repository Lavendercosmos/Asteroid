package se233.asteroid;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import se233.asteroid.view.GameView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.layout.StackPane;
import java.util.HashSet;
import java.util.Set;

public class Launcher extends Application {
    private static final Logger logger = LogManager.getLogger(Launcher.class);
    private GameView gameView;
    private Scene scene;
    private Set<KeyCode> pressedKeys;
    private StackPane rootPane;

    @Override
    public void start(Stage stage) {
        try {
            // Initialize components
            gameView = new GameView();
            pressedKeys = new HashSet<>();

            // Create root pane for centering
            rootPane = new StackPane();
            rootPane.getChildren().add(gameView);

            // Get screen dimensions
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Use fixed dimensions instead of calculating from screen size
            double fixedWidth = GameView.DEFAULT_WIDTH;
            double fixedHeight = GameView.DEFAULT_HEIGHT;

            // Create scene with fixed dimensions
            scene = new Scene(rootPane, fixedWidth, fixedHeight);

            // Setup controls
            setupControls();

            // Configure stage
            stage.setTitle("Asteroid Game");
            stage.setScene(scene);
            stage.setResizable(false); // Disable resizing

            // Center the stage on screen
            stage.setX((screenBounds.getWidth() - fixedWidth) / 2);
            stage.setY((screenBounds.getHeight() - fixedHeight) / 2);

            // Setup start button
            gameView.getStartButton().setOnAction(e -> gameView.startGame());

            stage.show();

            logger.info("Game initialized with fixed dimensions: {}x{}",
                    fixedWidth, fixedHeight);
        } catch (Exception e) {
            logger.error("Failed to start game", e);
        }
    }

    private void setupControls() {
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());

            switch (e.getCode()) {
                case SPACE:
                    if (gameView.isGameStarted() && !gameView.isPaused()) {
                        gameView.shoot();
                        logger.debug("Shoot action triggered");
                    }
                    break;
                case ESCAPE:
                    if (gameView.isGameStarted()) {
                        if (gameView.isPaused()) {
                            gameView.resumeGame();
                        } else {
                            gameView.pauseGame();
                        }
                    }
                    break;
                case F11:
                    Stage stage = (Stage) scene.getWindow();
                    stage.setFullScreen(!stage.isFullScreen());
                    break;

                    case F:
                        if (gameView.isGameStarted() && !gameView.isPaused()) {
                            gameView.Specialshoot();
                            logger.debug("Spacial shoot action triggered");
                        }
                        break;
            }
        });

        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            if (e.getCode() == KeyCode.W) {
                gameView.stopThrust();
                logger.debug("Thrust stopped");
            }
        });

        // Setup continuous input handling
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                handleContinuousInput();
            }
        }.start();
    }

    private void handleContinuousInput() {
        if (!gameView.isGameStarted() || gameView.isPaused()) return;

        if (pressedKeys.contains(KeyCode.Q)) {
            gameView.rotateLeft();
            logger.debug("Rotating left");
        }
        if (pressedKeys.contains(KeyCode.E)) {
            gameView.rotateRight();
            logger.debug("Rotating right");
        }
        if (pressedKeys.contains(KeyCode.W)) {
            gameView.moveUp();
            logger.debug("Moving up");
        }
        if (pressedKeys.contains(KeyCode.S)) {
            gameView.moveDown();
            logger.debug("Moving down");
        }
        if (pressedKeys.contains(KeyCode.A)) {
            gameView.moveLeft();
            logger.debug("Moving left");
        }
        if (pressedKeys.contains(KeyCode.D)) {
            gameView.moveRight();
            logger.debug("Moving right");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}