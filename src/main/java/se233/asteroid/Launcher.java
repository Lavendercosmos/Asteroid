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
            double initialWidth = Math.min(GameView.DEFAULT_WIDTH, screenBounds.getWidth() * 0.8);
            double initialHeight = Math.min(GameView.DEFAULT_HEIGHT, screenBounds.getHeight() * 0.8);

            // Create scene
            scene = new Scene(rootPane, initialWidth, initialHeight);

            // Setup controls
            setupControls();

            // Configure stage
            stage.setTitle("Asteroid Game");
            stage.setScene(scene);
            stage.setResizable(true);

            // Center the stage
            stage.setX((screenBounds.getWidth() - initialWidth) / 2);
            stage.setY((screenBounds.getHeight() - initialHeight) / 2);

            // Setup resize handlers
            setupResizeHandlers(stage);

            // Setup start button
            gameView.getStartButton().setOnAction(e -> gameView.startGame());

            stage.show();

            logger.info("Game initialized successfully with dimensions: {}x{}",
                    initialWidth, initialHeight);
        } catch (Exception e) {
            logger.error("Failed to start game", e);
        }
    }

    private void setupResizeHandlers(Stage stage) {
        // Listen for window resize
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            gameView.handleResize(newVal.doubleValue(), stage.getHeight());
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            gameView.handleResize(stage.getWidth(), newVal.doubleValue());
        });

        // Set minimum window size
        stage.setMinWidth(GameView.DEFAULT_WIDTH * 0.5);
        stage.setMinHeight(GameView.DEFAULT_HEIGHT * 0.5);
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

        if (pressedKeys.contains(KeyCode.A)) {
            gameView.rotateLeft();
            logger.debug("Rotating left");
        }
        if (pressedKeys.contains(KeyCode.D)) {
            gameView.rotateRight();
            logger.debug("Rotating right");
        }
        if (pressedKeys.contains(KeyCode.W)) {
            gameView.thrust();
            logger.debug("Thrusting");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}